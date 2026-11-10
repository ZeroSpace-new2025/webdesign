package com.university.webdesign.impl.user;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.common.model.PermissionList;
import com.university.webdesign.domain.user.Role;
import com.university.webdesign.domain.user.User;
import com.university.webdesign.domain.user.UserStatus;
import com.university.webdesign.repository.user.RoleRepository;
import com.university.webdesign.repository.user.UserRepository;
import com.university.webdesign.service.user.AuthService;
import com.university.webdesign.service.user.dto.LoginVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：已签发的 JWT 不能绕过账号状态与权限变更。
 * <p>
 * 缺陷背景：{@code AuthService.verifyToken} 只校验签名、黑名单与改密时间戳，
 * 直接信任 token 里的 {@code roles} / {@code permCodes}。于是：
 * <ul>
 *     <li>账号被停用/锁定后，旧 token 仍可用满有效期（默认 12 小时）；</li>
 *     <li>角色上撤销某个权限点后，旧 token 依然能通过 {@code @RequiresPerm}。</li>
 * </ul>
 * 修复后 {@code verifyToken} 以库里的账号状态与角色权限为准。
 */
@SpringBootTest
class AuthTokenStatusTests
{
	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Test
	void disabledAccountTokenIsRejected() {
		LoginVO login = authService.login("finance", "finance123");
		String token = login.getToken();
		UserContext context = authService.verifyToken(token);
		Long userId = context.userId();
		assertEquals(2L, userId, "种子账号 finance 的固定 ID 为 2");

		User user = userRepository.findWithRolesById(userId).orElseThrow();
		UserStatus original = user.getStatus();
		try {
			user.setStatus(UserStatus.DISABLED);
			userRepository.saveAndFlush(user);

			BusinessException exception = assertThrows(BusinessException.class,
					() -> authService.verifyToken(token),
					"账号停用后，已签发的 token 必须立即失效");
			assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
		} finally {
			User restore = userRepository.findWithRolesById(userId).orElseThrow();
			restore.setStatus(original == null ? UserStatus.ACTIVE : original);
			userRepository.saveAndFlush(restore);
		}
	}

	@Test
	void reloginRightAfterLogoutInTheSameSecondIsUsable() {
		LoginVO first = authService.login("lisi", "123456");
		authService.logout(first.getToken());

		// 同一秒内重新登录：令牌必须与已拉黑的那条不同，且能正常使用
		LoginVO second = authService.login("lisi", "123456");
		assertNotEquals(first.getToken(), second.getToken(),
				"同一秒内重新签发的令牌不能与已登出的令牌相同");
		assertEquals(6L, authService.verifyToken(second.getToken()).userId(),
				"重新登录拿到的令牌必须可用");
	}

	@Test
	void permissionRevocationAppliesToExistingToken() {
		LoginVO login = authService.login("finance", "finance123");
		String token = login.getToken();
		assertTrue(authService.verifyToken(token).hasPermission(PermissionEnum.REPORT_VIEW),
				"FINANCE 预置角色带 report:view");

		Role role = roleRepository.findByNameIgnoreCase("FINANCE").orElseThrow();
		PermissionList original = role.getPermissionList();
		PermissionList reduced = new PermissionList();
		List<PermissionEnum> kept = new ArrayList<>();
		for (PermissionEnum permission : original) {
			if (permission != PermissionEnum.REPORT_VIEW) {
				kept.add(permission);
			}
		}
		reduced.setRoles(kept);
		try {
			role.setPermissionList(reduced);
			roleRepository.saveAndFlush(role);

			assertFalse(authService.verifyToken(token).hasPermission(PermissionEnum.REPORT_VIEW),
					"角色上撤销权限点后，旧 token 不应再有该权限");
		} finally {
			Role restore = roleRepository.findByNameIgnoreCase("FINANCE").orElseThrow();
			restore.setPermissionList(original);
			roleRepository.saveAndFlush(restore);
		}
	}
}
