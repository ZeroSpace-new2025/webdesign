package com.university.webdesign.impl.user;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.domain.user.Role;
import com.university.webdesign.domain.user.User;
import com.university.webdesign.domain.user.UserStatus;
import com.university.webdesign.repository.user.UserRepository;
import com.university.webdesign.impl.user.support.JwtCodec;
import com.university.webdesign.impl.user.support.JwtPayload;
import com.university.webdesign.service.user.AuthService;
import com.university.webdesign.service.user.dto.CurrentUserVO;
import com.university.webdesign.service.user.dto.LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证与授权服务实现。
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService
{
	private static final int MIN_PASSWORD_LENGTH = 6;

	private final Map<String, Instant> tokenBlacklist = new ConcurrentHashMap<>();
	private final Map<Long, Instant> passwordChangedAt = new ConcurrentHashMap<>();

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtCodec jwtCodec;

	public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
	                       JwtCodec jwtCodec) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtCodec = jwtCodec;
	}

	@Override
	@Transactional
	public LoginVO login(String employeeNo, String rawPassword) {
		String normalized = trimToNull(employeeNo);
		if (normalized == null || rawPassword == null || rawPassword.isEmpty()) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "工号与密码不能为空");
		}

		User user = userRepository.findWithRolesByEmployeeNoIgnoreCase(normalized)
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED, "工号或密码错误"));
		assertLoginable(user);
		if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
			log.warn("登录失败：工号={}，密码不匹配", normalized);
			throw new BusinessException(ErrorCode.UNAUTHENTICATED, "工号或密码错误");
		}
		CurrentUserVO userInfo = toCurrentUser(user);
		String token = jwtCodec.sign(user.getId(), user.getEmployeeNo(), user.getName(),
				user.getDeptId(), userInfo.getRoles(), userInfo.getPermCodes());
		long expiresIn = jwtCodec.expireSeconds();
		log.info("登录成功：工号={}，用户ID={}", user.getEmployeeNo(), user.getId());
		return LoginVO.of(token, expiresIn, Instant.now().plusSeconds(expiresIn).toEpochMilli(), userInfo);
	}

	@Override
	public void logout(String token) {
		String raw = JwtCodec.unify(token);
		if (raw.isEmpty()) {
			return;
		}
		try {
			JwtPayload payload = jwtCodec.parse(raw);
			tokenBlacklist.put(raw, Instant.ofEpochSecond(payload.expiresAt()));
			log.info("登出：用户ID={}，令牌已加入黑名单", payload.userId());
		} catch (BusinessException exception) {
			log.debug("登出时令牌已失效：{}", exception.getMessage());
		}
	}

	@Override
	@Transactional
	public CurrentUserVO currentUser(Long userId) {
		if (userId == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "用户ID不能为空");
		}
		UserContext context = UserContextHolder.get();
		if (context != null && context.userId() != null && !context.userId().equals(userId)
				&& !context.hasAnyRole(RoleCodes.MANAGER)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "只能查看本人的登录信息");
		}
		User user = userRepository.findWithRolesById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "员工不存在：" + userId));
		return toCurrentUser(user);
	}

	@Override
	@Transactional
	public void changePassword(Long userId, String oldPwd, String newPwd) {
		if (userId == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "用户ID不能为空");
		}
		if (oldPwd == null || oldPwd.isEmpty()) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "旧密码不能为空");
		}
		if (oldPwd.equals(newPwd)) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "新密码不能与旧密码相同");
		}
		if (newPwd == null || newPwd.length() < MIN_PASSWORD_LENGTH) {
			throw new BusinessException(ErrorCode.PARAM_INVALID,
					"新密码长度不能少于 " + MIN_PASSWORD_LENGTH + " 位");
		}
		User user = userRepository.findWithRolesById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "员工不存在：" + userId));
		if (!passwordEncoder.matches(oldPwd, user.getPassword())) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "旧密码不正确");
		}
		user.setPassword(passwordEncoder.encode(newPwd));
		userRepository.save(user);
		passwordChangedAt.put(user.getId(), Instant.now().truncatedTo(ChronoUnit.SECONDS));
		log.info("修改密码成功：用户ID={}", user.getId());
	}

	@Override
	@Transactional
	public UserContext verifyToken(String token) {
		String raw = JwtCodec.unify(token);
		JwtPayload payload = jwtCodec.parse(raw);
		Instant blacklisted = tokenBlacklist.get(raw);
		if (blacklisted != null) {
			if (blacklisted.isAfter(Instant.now())) {
				throw new BusinessException(ErrorCode.UNAUTHENTICATED, "登录已失效，请重新登录");
			}
			tokenBlacklist.remove(raw);
		}
		Instant changed = passwordChangedAt.get(payload.userId());
		if (changed != null && payload.issuedAt() < changed.getEpochSecond()) {
			throw new BusinessException(ErrorCode.UNAUTHENTICATED, "密码已修改，请重新登录");
		}
		// 账号状态与角色权限以库为准：JWT 里的 claims 只是签发那一刻的快照，
		// 不能当作授权依据。否则“停用账号”只能挡住新的登录，已签发的 token 仍可用满有效期
		// （默认 12 小时）；同理，角色上撤销某个权限点后，旧 token 依然能过 @RequiresPerm。
		User user = userRepository.findWithRolesById(payload.userId())
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED, "登录已失效，请重新登录"));
		UserStatus status = user.getStatus() == null ? UserStatus.ACTIVE : user.getStatus();
		if (status != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.UNAUTHENTICATED,
					"账号" + status.getText() + "，请联系管理员");
		}
		List<String> roles = new ArrayList<>();
		Set<String> permCodes = new LinkedHashSet<>();
		for (Role role : user.getRoles()) {
			if (role == null) {
				continue;
			}
			roles.add(role.getName());
			permCodes.addAll(role.permCodes());
		}
		return new UserContext(user.getId(), user.getEmployeeNo(), user.getName(), user.getDeptId(),
				user.getWorkstation(), user.getPhone(), List.copyOf(roles), List.copyOf(permCodes));
	}

	@Override
	@Transactional
	public boolean checkPermission(Long userId, PermissionEnum permCode) {
		if (userId == null || permCode == null || permCode == PermissionEnum.None) {
			return false;
		}
		User user = userRepository.findWithRolesById(userId).orElse(null);
		if (user == null || !user.isActive()) {
			return false;
		}
		for (Role role : user.getRoles()) {
			if (role != null && role.permissions().hasPermission(permCode)) {
				return true;
			}
		}
		return false;
	}

	private void assertLoginable(User user) {
		UserStatus status = user.getStatus() == null ? UserStatus.ACTIVE : user.getStatus();
		switch (status) {
			case DISABLED -> throw new BusinessException(ErrorCode.FORBIDDEN, "账号已停用，请联系管理员");
			case LOCKED -> throw new BusinessException(ErrorCode.FORBIDDEN, "账号已锁定，请联系管理员");
			case ACTIVE -> {
				// 正常状态，继续校验密码
			}
			default -> throw new BusinessException(ErrorCode.FORBIDDEN, "账号状态异常，无法登录");
		}
	}

	private CurrentUserVO toCurrentUser(User user) {
		CurrentUserVO vo = new CurrentUserVO();
		vo.setUserId(user.getId());
		vo.setEmployeeNo(user.getEmployeeNo());
		vo.setName(user.getName());
		vo.setDeptId(user.getDeptId());
		vo.setDeptName(user.getDeptName());
		vo.setWorkstation(user.getWorkstation());
		vo.setPhone(user.getPhone());
		UserStatus status = user.getStatus() == null ? UserStatus.ACTIVE : user.getStatus();
		vo.setStatus(status.name());
		vo.setStatusText(status.getText());
		List<String> roles = new ArrayList<>();
		Set<String> permCodes = new LinkedHashSet<>();
		for (Role role : user.getRoles()) {
			if (role == null) {
				continue;
			}
			roles.add(role.getName());
			permCodes.addAll(role.permCodes());
		}
		vo.setRoles(roles);
		vo.setPermCodes(new ArrayList<>(permCodes));
		return vo;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}
}