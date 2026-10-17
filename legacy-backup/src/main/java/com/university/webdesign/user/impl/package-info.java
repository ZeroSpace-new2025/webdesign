/**
 * 用户与报表中心 —— 服务实现层。
 * <p>
 * 放置 {@code com.university.webdesign.user.service} 与本模块报表服务的实现类，
 * 命名“接口名 + Impl”，用 {@code @Component} 注册。
 * <p>
 * 对接注意事项：
 * <ul>
 *     <li>{@code UserService.hasRole} / {@code hasAnyRole} 已被订单模块用于越权校验，
 *         角色编码需与订单模块对齐（订单模块暂用 {@code MANAGER} 与 {@code FINANCE}）；</li>
 *     <li>登录认证落地后，订单模块的 {@code operatorId} 应改为从认证上下文获取，
 *         不再由前端传入。</li>
 * </ul>
 */
package com.university.webdesign.user.impl;
