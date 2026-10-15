package com.university.webdesign.api.user;

import lombok.Data;

import java.util.List;

/**
 * 分配用户角色请求（M4-20）。
 * <p>
 * 全量覆盖：提交的 {@code roleIds} 即为该用户最终的角色集合，为空表示清空角色。
 */
@Data
public class RoleAssignRequest
{
	/**
	 * 角色ID集合
	 */
	private List<Long> roleIds;
}
