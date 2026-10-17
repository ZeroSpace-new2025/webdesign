package com.university.webdesign.reporting.api;

import com.university.webdesign.common.Result;
import com.university.webdesign.reporting.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

/**
 * 财务报表API。
 */
@RestController
@RequestMapping("/api/report")
public class ReportApi
{
	private final ReportService reportService;

	public ReportApi(ReportService reportService) {
		this.reportService = reportService;
	}

	/**
	 * 未传月份时默认统计当前月份。
	 */
	@GetMapping("/monthly")
	public Result<MonthlySalesReportDTO> monthly(@RequestParam(required = false) String month) {
		return Result.success(reportService.getMonthlySalesReport(parseMonth(month)));
	}

	/**
	 * 返回指定月份全部员工的消费汇总。
	 */
	@GetMapping("/employees")
	public Result<List<EmployeeConsumptionReportDTO>> employees(@RequestParam(required = false) String month) {
		return Result.success(reportService.queryEmployeeConsumptionReports(parseMonth(month)));
	}

	/**
	 * 返回单个员工的汇总和订单明细。
	 */
	@GetMapping("/employee/{userId}")
	public Result<EmployeeConsumptionReportDTO> employee(
		@PathVariable("userId") Long userId,
		@RequestParam(required = false) String month) {
		return Result.success(reportService.getEmployeeConsumptionReport(userId, parseMonth(month)));
	}

	/**
	 * 前端月份选择器使用的数据来源。
	 */
	@GetMapping("/months")
	public Result<List<YearMonth>> months() {
		return Result.success(reportService.getAvailableReportMonths());
	}

	/**
	 * 手动刷新月度统计缓存。
	 */
	@PostMapping("/refresh")
	public Result<Void> refresh(@RequestParam(required = false) String month) {
		reportService.refreshMonthlyReport(parseMonth(month));
		return Result.success(null);
	}

	/**
	 * 统一处理 yyyy-MM 格式错误。
	 */
	private YearMonth parseMonth(String month) {
		if (month == null || month.isBlank()) {
			return YearMonth.now();
		}
		try {
			return YearMonth.parse(month);
		} catch (Exception exception) {
			throw new IllegalArgumentException("月份格式应为 yyyy-MM");
		}
	}
}
