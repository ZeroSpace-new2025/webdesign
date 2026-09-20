package com.university.webdesign.reporting.service;

import com.university.webdesign.reporting.api.EmployeeConsumptionReportDTO;
import com.university.webdesign.reporting.api.MonthlySalesReportDTO;

import java.time.YearMonth;
import java.util.List;

/**
 * 财务报表服务。
 */
public interface ReportService
{
	/**
	 * 获取餐厅指定月份的销售总报表。
	 */
	MonthlySalesReportDTO getMonthlySalesReport(YearMonth month);

	/**
	 * 获取指定员工在指定月份的消费汇总与订单明细。
	 */
	EmployeeConsumptionReportDTO getEmployeeConsumptionReport(Long userId, YearMonth month);

	/**
	 * 获取已有月度报表的月份列表。
	 */
	List<YearMonth> getAvailableReportMonths();

	/**
	 * 重新汇总并刷新指定月份的报表缓存。
	 */
	void refreshMonthlyReport(YearMonth month);
}
