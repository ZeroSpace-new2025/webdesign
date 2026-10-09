package com.university.webdesign.service.user.dto;

import com.university.webdesign.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色分页查询条件。
 * <p>
 * 对应《对外方法表》M4-14 `GET /roles`：按关键字（角色名称）分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQuery extends PageQuery
{
	/**
	 * 关键字，模糊匹配角色名称，为空表示不限
	 */
	private String keyword;
}
