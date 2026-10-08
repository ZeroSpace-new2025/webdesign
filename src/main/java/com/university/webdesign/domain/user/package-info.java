package com.university.webdesign.domain.user;

/**
 * 用户与报表中心（M4）——用户、角色、权限的领域包。
 * <p>
 * 实体：{@link User}（`users`）、{@link Role}（`roles`）、{@link Permission}（`permissions`）；
 * 枚举：{@link UserStatus}。
 * <p>
 * 领域事件（`UserCreatedEvent` / `UserUpdatedEvent` / `RolePermissionChangedEvent`）
 * 由 api 与 service 层通过 {@code com.university.webdesign.event} 包统一发布与订阅，
 * 因此本包不再重复定义事件类。
 */
