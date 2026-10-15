package com.university.webdesign.service.report;

/**
 * 用户与报表中心（M4）——财务报表的服务契约包。
 * <p>
 * 对外接口：{@link ReportService}（月度销售报表生成/查询/导出/刷新）、
 * {@link ConsumptionAuditService}（员工消费审计、部门消费汇总）；
 * {@link MonthConverter} 提供 {@code yyyy-MM} 文本与 {@code YearMonth} 的转换。
 * <p>
 * 跨层入参出参 DTO/VO 放在 {@code service.report.dto} 子包；
 * 实现类在 {@code service.impl.report}。
 */
