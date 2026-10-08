package com.university.webdesign.user.service;

/**
 * 角色授权测试开关。
 * <p>
 * 订单模块的集成测试使用不存在的用户ID验证“经理/财务可越权”的分支，
 * 测试期间通过 {@code StubServicesTestConfiguration} 临时授予全部角色。
 * 生产环境不会打开该开关，角色判断仍以数据库中的用户角色为准。
 */
public final class RoleAuthorizationTestSupport
{
	private static volatile boolean grantAllRoles;

	private RoleAuthorizationTestSupport() {
	}

	/**
	 * 设置测试期间的“授予全部角色”开关。
	 *
	 * @param granted true 表示所有用户都拥有任意角色
	 */
	public static void grantAllRoles(boolean granted) {
		grantAllRoles = granted;
	}

	/**
	 * 供用户服务实现读取测试开关。
	 *
	 * @return 是否处于“授予全部角色”的测试状态
	 */
	static boolean isGrantAllRoles() {
		return grantAllRoles;
	}
}
