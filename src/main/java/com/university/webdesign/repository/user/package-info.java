package com.university.webdesign.repository.user;

/**
 * 用户与报表中心（M4）——用户/角色/权限的数据访问包。
 * <p>
 * 只放本模块独占表的 Spring Data JPA 仓库：`users`（{@link UserRepository}）、
 * `role`（{@link RoleRepository}，权限为 {@code permission_list} 位图，不再有权限点表）。
 * 其它模块（M2 订单、M3 配送）取员工信息必须经 `service.user.UserService` 契约，
 * 不得依赖本包。
 */
