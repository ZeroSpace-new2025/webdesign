package com.university.webdesign.support;

import com.university.webdesign.user.service.RoleAuthorizationTestSupport;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * 测试辅助配置。
 * <p>
 * 各模块的 service 实现类已陆续落地：菜单中心的 {@code MenuServiceImpl} / {@code RecipeServiceImpl}
 * 与用户中心的 {@code UserServiceImpl} 现在都是真实的 Spring Bean，
 * 原先的菜单、菜谱、用户桩实现已随之删除（保留桩会与真实 Bean 冲突，
 * 令测试中的 {@code @MockitoBean} 无法确定要覆盖哪一个）。
 * <p>
 * 目前仅剩一个职责：部分订单测试使用不在种子数据中的用户ID验证经理/财务越权分支，
 * 这里提供 {@link #grantAllRoles(boolean)} 显式切换权限开关。
 */
@TestConfiguration
public class StubServicesTestConfiguration
{
	/**
	 * 开关“授予所有角色”
	 * <p>
	 * 订单测试使用的用户ID不一定存在于用户表中，因此通过测试专用开关
	 * 让用户服务在测试期间授予全部角色。
	 * 用完记得在测试结束（{@code @AfterEach}）复位，避免影响其他测试。
	 *
	 * @param granted true 表示所有用户都拥有任意角色
	 */
	public static void grantAllRoles(boolean granted) {
		RoleAuthorizationTestSupport.grantAllRoles(granted);
	}
}
