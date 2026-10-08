package com.university.webdesign.common;

/**
 * 全项目统一的角色编码常量。
 * <p>
 * 对应《重构实施规范》2.1：角色编码是全项目共享的极小常量，因此放在 {@code common} 包，
 * 各模块（M1~M4）只依赖本类，不再各自声明字符串字面量。
 * <p>
 * 角色取值与 {@code demand.md} 的业务角色一一对应：
 * 餐厅经理、厨房主管、配餐员、财务管理、企业员工。
 */
public final class RoleCodes
{
	/**
	 * 餐厅经理（系统管理员，拥有全部权限）
	 */
	public static final String MANAGER = "MANAGER";

	/**
	 * 厨房主管
	 */
	public static final String KITCHEN_SUPERVISOR = "KITCHEN_SUPERVISOR";

	/**
	 * 配餐员
	 */
	public static final String DELIVERY_STAFF = "DELIVERY_STAFF";

	/**
	 * 财务管理（可查他人历史与消费明细、导出报表）
	 */
	public static final String FINANCE = "FINANCE";

	/**
	 * 企业员工
	 */
	public static final String EMPLOYEE = "EMPLOYEE";

	/**
	 * 全部预置角色编码（用户中心做数据初始化与“预置角色不可删除”校验时使用）
	 */
	public static final String[] ALL = {
			MANAGER, KITCHEN_SUPERVISOR, DELIVERY_STAFF, FINANCE, EMPLOYEE
	};

	private RoleCodes() {
	}
}
