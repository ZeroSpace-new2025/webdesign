/**
 * 运营与履约系统 —— 服务接口层。
 * <p>
 * 放置本模块对外提供的能力接口（总括订单聚合、生产单打印、配送单批量生成等）。
 * 跨模块调用方只允许依赖本包与 {@code api} 包。
 * <p>
 * 本模块是订单数据的下游：只读取订单模块的服务，不直接读写订单表。
 * 已备好可直接使用的入口（{@code com.university.webdesign.ordertransaction.service.OrderService}）：
 * <ul>
 *     <li>{@code query(OrderQueryData)}：按时间范围等条件取订单；</li>
 *     <li>{@code getTodayOrders()}：取当日有效订单；</li>
 *     <li>{@code isBeforeCutoff(LocalDate)}：判断是否已过订餐截止时间（聚合的生产单只在截止后生成）；</li>
 *     <li>{@code findActiveOrder(userId, workDate)}：按员工+日期取有效订单，供配送单使用。</li>
 * </ul>
 */
package com.university.webdesign.operationfulfillment.service;
