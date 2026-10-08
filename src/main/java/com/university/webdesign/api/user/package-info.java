package com.university.webdesign.api.user;

/**
 * 用户与报表中心（M4）——对外 REST 控制器包（基础路径 {@code /api/v1/user}）。
 * <p>
 * 控制器：{@link ApiAuthController}（认证）、{@link ApiUserController}（员工维护）、
 * {@link ApiRoleController}（角色与权限）、{@link ApiReportController}（财务报表），
 * 共 26 个接口，与《对外方法表》5.1 一一对应。
 * <p>
 * 本包只做协议转换与 Result 组装，业务规则与数据归属校验在 service 层；
 * 请求/响应 DTO 与控制器同包，跨层契约 DTO 在 {@code service.*.dto}。
 */
