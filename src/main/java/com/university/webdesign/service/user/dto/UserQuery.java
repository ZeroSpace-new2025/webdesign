package com.university.webdesign.service.user.dto;

import com.university.webdesign.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询条件。
 * <p>
 * 对应《对外方法表》M4-06 `GET /users`：按姓名/部门/工位/状态筛选。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQuery extends PageQuery
{
	/**
	 * 关键字，模糊匹配工号/姓名/工位/电话，为空表示不限
	 */
	private String keyword;

	/**
	 * 部门ID，为空表示不限
	 */
	private Long deptId;

	/**
	 * 工位，模糊匹配，为空表示不限
	 */
	private String workstation;

	/**
	 * 账号状态编码 ACTIVE / DISABLED / LOCKED，为空表示不限
	 */
	private String status;
}
