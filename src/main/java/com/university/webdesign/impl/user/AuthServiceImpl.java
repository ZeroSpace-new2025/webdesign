package com.university.webdesign.impl.user;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.config.TemporaryAdminAccount;
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
 * <p>
 * 对应《对外方法表》5.2 的 {@code AuthService} 全部方法：JWT（HS256）签发与校验、
 * 当前用户上下文、改密、权限点判定。
 * <p>
 * 模块自足（见《重构实施规范》第 6 节的环形依赖说明）：本类只依赖 {@code common}、
 * 本模块的实体/仓库/DTO 与 JDK，**不依赖 M2/M3 的任何类型**，
 * 因此 {@code OrderService} 可以安全地注入 {@code AuthService}。
 * <p>
 * 事务统一用普通 {@code @Transactional}（含查询），不加 {@code readOnly = true}。
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService
{
	/**
	 * 密码最短长度
	 */
	private static final int MIN_PASSWORD_LENGTH = 6;

	/**
	 * 登出黑名单：token → 该 token 的到期时刻。
	 * <p>
	 * 生产建议换 Redis：多实例部署时进程内 Map 无法跨节点共享，节点重启即丢失黑名单，
	 * 且 token 数量增长会占用堆内存。当前为课程项目的单实例部署，先用内存实现。
	 */
	private final Map<String, Instant> tokenBlacklist = new ConcurrentHashMap<>();

	/**
	 * 改密时间戳：userId → 最后一次改密时刻，用于让此前签发的 token 立即失效。
	 * <p>
	 * 同样建议生产环境换 Redis（键 `auth:password-changed:{userId}`）。
	 */
	private final Map<Long, Instant> passwordChangedAt = new ConcurrentHashMap<>();

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtCodec jwtCodec;
	private final TemporaryAdminAccount temporaryAdmin;

	/**
	 * 构造器注入
	 *
	 * @param userRepository  用户仓库（含角色与权限）
	 * @param passwordEncoder BCrypt 密码编码器
	 * @param jwtCodec        JWT 编解码器
	 * @param temporaryAdmin  临时硬编码管理员账号（课程演示用，见 {@link TemporaryAdminAccount}）
	 */
	public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
			JwtCodec jwtCodec, TemporaryAdminAccount temporaryAdmin) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtCodec = jwtCodec;
		this.temporaryAdmin = temporaryAdmin;
	}

	@Override
	@Transactional
	public LoginVO login(String employeeNo, String rawPassword) {
		String normalized = trimToNull(employeeNo);
		if (normalized == null || rawPassword == null || rawPassword.isEmpty()) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "工号与密码不能为空");
		}
		// 临时硬编码管理员：不查库，直接签发全权限令牌（app.storage=none 时也能登录）
		if (temporaryAdmin.matches(normalized, rawPassword)) {
			CurrentUserVO adminInfo = temporaryAdminInfo();
			String adminToken = jwtCodec.sign(TemporaryAdminAccount.USER_ID, temporaryAdmin.employeeNo(),
					adminInfo.getName(), null, adminInfo.getRoles(), adminInfo.getPermCodes());
			long adminExpiresIn = jwtCodec.expireSeconds();
			log.warn("临时硬编码管理员登录：工号={}", temporaryAdmin.employeeNo());
			return LoginVO.of(adminToken, adminExpiresIn,
					Instant.now().plusSeconds(adminExpiresIn).toEpochMilli(), adminInfo);
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
			// 令牌本身已失效（过期/签名错误），无需再拉黑
			log.debug("登出时令牌已失效：{}", exception.getMessage());
		}
	}

	@Override
	@Transactional
	public CurrentUserVO currentUser(Long userId) {
		if (userId == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "用户ID不能为空");
		}
		if (temporaryAdmin.isAdmin(userId)) {
			return temporaryAdminInfo();
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
		if (temporaryAdmin.isAdmin(userId)) {
			throw new BusinessException(ErrorCode.PARAM_INVALID,
					"临时硬编码管理员账号不支持修改密码");
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
		// 改密后此前签发的 token 全部失效；截断到秒，与 JWT 载荷里的 iat（秒）同一精度比较
		passwordChangedAt.put(user.getId(), Instant.now().truncatedTo(ChronoUnit.SECONDS));
		log.info("修改密码成功：用户ID={}", user.getId());
	}

	@Override
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
		return new UserContext(payload.userId(), payload.employeeNo(), payload.name(), payload.deptId(),
				null, null, List.copyOf(payload.roles()), List.copyOf(payload.permCodes()));
	}

	@Override
	@Transactional
	public boolean checkPermission(Long userId, PermissionEnum permCode) {
		if (userId == null || permCode == null || permCode == PermissionEnum.None) {
			return false;
		}
		// 临时硬编码管理员不落库，权限直接由配置给出（全部细粒度权限点）
		if (temporaryAdmin.isAdmin(userId)) {
			return temporaryAdmin.permCodes().stream()
					.anyMatch(code -> code.equalsIgnoreCase(permCode.displayCode()));
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

	/**
	 * 登录前置校验：账号必须存在且为启用状态
	 *
	 * @param user 用户实体
	 */
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

	/**
	 * 用户实体 → 当前用户视图（含角色与展开后的权限点）
	 *
	 * @param user 用户实体
	 * @return 当前用户视图
	 */
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

	/**
	 * 临时硬编码管理员的当前用户视图（不查库，权限取全部细粒度权限点）
	 *
	 * @return 当前用户视图
	 */
	private CurrentUserVO temporaryAdminInfo() {
		CurrentUserVO vo = new CurrentUserVO();
		vo.setUserId(TemporaryAdminAccount.USER_ID);
		vo.setEmployeeNo(temporaryAdmin.employeeNo());
		vo.setName(TemporaryAdminAccount.NAME);
		vo.setStatus(UserStatus.ACTIVE.name());
		vo.setStatusText(UserStatus.ACTIVE.getText());
		vo.setRoles(temporaryAdmin.roles());
		vo.setPermCodes(temporaryAdmin.permCodes());
		return vo;
	}

	/**
	 * 去空白，空白返回 null
	 *
	 * @param value 原值
	 * @return 归一化后的值
	 */
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}
}
