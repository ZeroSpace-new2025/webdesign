package com.university.webdesign.service.user;

/**
 * 用户与报表中心（M4）——用户与认证的服务契约包。
 * <p>
 * 对外接口：{@link UserService}（员工维护 + 跨模块批量查询 + {@code hasAnyRole}）、
 * {@link RoleService}（角色与权限）、{@link AuthService}（JWT 认证与权限点判定）。
 * <p>
 * 跨层入参出参 DTO/VO 放在 {@code service.user.dto} 子包；
 * 实体与枚举在 {@code domain.user}，实现类在 {@code service.impl.user}。
 */
