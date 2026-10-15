package com.university.webdesign.service.user.dto;

/**
 * 员工精简视图。
 * <p>
 * 对应《对外方法表》5.2 的 {@code UserService.listByIds} / {@code getBrief}：
 * 供 M2 订单列表补齐员工姓名/部门、M3 配送单补齐工位/电话、M4 报表补齐部门归属。
 * 不含密码、角色等敏感信息。
 * <p>
 * 采用 record 形式（与 {@code service.*.dto} 中其他只读视图一致），
 * 同时提供 JavaBean 风格的 {@code getXxx()} 读取方法，兼容不同调用方的写法。
 *
 * @param userId      用户ID
 * @param employeeNo  工号
 * @param name        员工姓名
 * @param deptId      部门ID
 * @param deptName    部门名称快照
 * @param workstation 工位
 * @param phone       联系电话
 */
public record UserBriefVO(
		Long userId,
		String employeeNo,
		String name,
		Long deptId,
		String deptName,
		String workstation,
		String phone)
{
	/**
	 * @return 用户ID（JavaBean 风格）
	 */
	public Long getUserId() {
		return userId;
	}

	/**
	 * @return 工号（JavaBean 风格）
	 */
	public String getEmployeeNo() {
		return employeeNo;
	}

	/**
	 * @return 员工姓名（JavaBean 风格）
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return 部门ID（JavaBean 风格）
	 */
	public Long getDeptId() {
		return deptId;
	}

	/**
	 * @return 部门名称（JavaBean 风格）
	 */
	public String getDeptName() {
		return deptName;
	}

	/**
	 * @return 工位（JavaBean 风格）
	 */
	public String getWorkstation() {
		return workstation;
	}

	/**
	 * @return 联系电话（JavaBean 风格）
	 */
	public String getPhone() {
		return phone;
	}
}
