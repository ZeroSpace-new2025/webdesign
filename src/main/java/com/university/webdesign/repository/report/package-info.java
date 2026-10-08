package com.university.webdesign.repository.report;

/**
 * 用户与报表中心——报表模块（M4）的数据访问包。
 * <p>
 * 只放本模块独占表的 Spring Data JPA 仓库：`monthly_report` / `monthly_report_item`
 * （{@link MonthlyReportRepository}）。订单数据一律通过
 * {@code com.university.webdesign.service.order.OrderQueryService} 读取，
 * 禁止在此包内新建订单（`order_form` / `order_detail`）相关的仓库或查询。
 */
