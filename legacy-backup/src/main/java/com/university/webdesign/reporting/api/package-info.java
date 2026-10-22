/**
 * 用户与报表中心 —— 报表模块（对外接口层）。
 * <p>
 * 按 {@code demand.md}，财务报表属于本中心：月度销售总报表（全餐厅月度售出菜品数量与总金额）、
 * 员工消费审计（查询特定员工的月度订单汇总与消费明细）。
 * <p>
 * 报表数据来源：订单模块已经提供现成入口，不要直接读订单表——
 * {@code com.university.webdesign.ordertransaction.service.OrderService}：
 * <ul>
 *     <li>{@code query(OrderQueryData)}：按时间范围/用户/状态取订单（可组合成全餐厅月度汇总）；</li>
 *     <li>{@code getMonthlyConsumption(userId, operatorId, year, month)}：单个员工的月度消费统计
 *         （含逐单明细与按菜品汇总，已内置“只能查自己，经理/财务可查他人”的校验）。</li>
 * </ul>
 */
package com.university.webdesign.reporting.api;
