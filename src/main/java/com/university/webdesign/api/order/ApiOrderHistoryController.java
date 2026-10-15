package com.university.webdesign.api.order;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.Result;
import com.university.webdesign.service.order.OrderService;
import com.university.webdesign.service.order.OrderStatisticsService;
import com.university.webdesign.service.order.dto.ExportFormat;
import com.university.webdesign.service.order.dto.MonthlySummaryVO;
import com.university.webdesign.service.order.dto.OrderHistoryQuery;
import com.university.webdesign.service.order.dto.OrderVO;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

/**
 * 个人历史查询 REST 控制器。
 * <p>
 * 对应《对外方法表》M2 的 `ApiOrderHistoryController`（基础路径 `/api/v1/order`）：
 * 个人历史订单（M2-08）、月度消费统计（M2-09）、消费明细导出（M2-10）。
 * <p>
 * 查询对象固定为当前登录用户；经理与财务可通过 `employeeId` 查询他人（消费审计场景），
 * 越权判定在 service 层完成。
 */
@RestController
@RequestMapping("/api/v1/order")
public class ApiOrderHistoryController
{
	private final OrderService orderService;
	private final OrderStatisticsService orderStatisticsService;

	public ApiOrderHistoryController(OrderService orderService, OrderStatisticsService orderStatisticsService) {
		this.orderService = orderService;
		this.orderStatisticsService = orderStatisticsService;
	}

	/**
	 * M2-08 查询个人历史订单
	 *
	 * @param query 历史查询条件（日期区间、状态、分页）
	 * @return 分页订单
	 */
	@GetMapping("/orders/history")
	public Result<PageResult<OrderVO>> history(@ModelAttribute OrderHistoryQuery query) {
		return Result.success(orderService.pageHistory(query));
	}

	/**
	 * M2-09 个人月度消费统计
	 *
	 * @param employeeId 员工ID，为空取当前登录用户
	 * @param month      月份（`yyyy-MM`），为空取当月
	 * @return 月度汇总
	 */
	@GetMapping("/orders/history/monthly-summary")
	public Result<MonthlySummaryVO> monthlySummary(
			@RequestParam(value = "employeeId", required = false) Long employeeId,
			@RequestParam(value = "month", required = false) String month) {
		return Result.success(orderStatisticsService.monthlySummary(employeeId, parseMonth(month)));
	}

	/**
	 * M2-10 导出个人消费明细
	 *
	 * @param employeeId 员工ID，为空取当前登录用户
	 * @param month      月份（`yyyy-MM`）
	 * @param format     导出格式：XLSX / CSV
	 * @return 文件流
	 */
	@GetMapping("/orders/history/export")
	public ResponseEntity<Resource> export(
			@RequestParam(value = "employeeId", required = false) Long employeeId,
			@RequestParam(value = "month", required = false) String month,
			@RequestParam(value = "format", required = false, defaultValue = "XLSX") String format) {
		YearMonth target = parseMonth(month);
		ExportFormat exportFormat = ExportFormat.parse(format);
		Resource resource = orderStatisticsService.exportHistory(employeeId, target, exportFormat);
		String extension = exportFormat == ExportFormat.CSV ? "csv" : "xls";
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=consumption-" + target + "." + extension)
				.body(resource);
	}

	/**
	 * 解析 `yyyy-MM` 月份参数
	 *
	 * @param month 月份字符串，为空取当月
	 * @return 月份
	 */
	private YearMonth parseMonth(String month) {
		if (month == null || month.isBlank()) {
			return YearMonth.now();
		}
		try {
			return YearMonth.parse(month.trim());
		} catch (RuntimeException exception) {
			throw com.university.webdesign.common.BusinessException.paramInvalid("月份格式应为 yyyy-MM");
		}
	}
}
