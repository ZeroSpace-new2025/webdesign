package com.university.webdesign.api.user;

import lombok.Data;

import java.util.List;

/**
 * 配置角色权限请求（M4-18）。
 * <p>
 * 全量覆盖：提交的 {@code permCodes} 即为该角色最终的权限点集合，为空表示清空。
 */
@Data
public class PermissionAssignRequest
{
	/**
	 * 权限点编码集合
	 */
	private List<String> permCodes;
}
