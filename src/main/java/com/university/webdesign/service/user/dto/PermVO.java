package com.university.webdesign.service.user.dto;

/**
 * 权限点视图对象。
 * <p>
 * 对应《对外方法表》M4-17 `GET /permissions`：系统全部权限点，按模块分组。
 *
 * @param permCode    权限点编码（如 {@code report:view}）
 * @param permName    权限点名称
 * @param module      所属模块（menu / order / operation / user / report）
 * @param description 权限点说明
 */
public record PermVO(String permCode, String permName, String module, String description)
{
}
