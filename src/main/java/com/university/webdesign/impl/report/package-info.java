package com.university.webdesign.impl.report;

/**
 * 用户与报表中心（M4）——财务报表服务的实现包。
 * <p>
 * 实现类：{@link ReportServiceImpl}、{@link ConsumptionAuditServiceImpl}；
 * {@code support} 子包放订单取数适配（{@code MonthlyOrderReader}）与报表导出工具
 * （{@code ReportExportWriter}）。
 * <p>
 * 取数边界：订单数据只经 {@code service.order.OrderQueryService} 读取，
 * 不新建订单实体、不直接访问 {@code order_form} / {@code order_detail} 表。
 */
