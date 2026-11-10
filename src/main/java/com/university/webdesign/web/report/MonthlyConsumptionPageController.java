package com.university.webdesign.web.report;

import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.TabularExport;
import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.config.RequiresPerm;
import com.university.webdesign.service.report.ConsumptionAuditService;
import com.university.webdesign.service.report.MonthConverter;
import com.university.webdesign.service.report.dto.EmployeeConsumptionVO;
import com.university.webdesign.service.report.dto.EmployeeMonthlyConsumptionVO;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 员工月度消费报表页面控制器（服务端渲染，M4 用户与报表中心）。
 * <p>
 * 页面路由与 REST 控制器分开：本类走 `/reports/monthly/**` 返回视图或 CSV 文件，
 * 报表数据一律来自 {@link ConsumptionAuditService}（权限、取数口径都在 service 层），
 * 页面只做展示与协议转换，不自行聚合业务数据。
 * <ul>
 *     <li>`GET /reports/monthly`：按月就餐日期统计的个人消费汇总表；</li>
 *     <li>`GET /reports/monthly/export`：同一份汇总表的 CSV 下载（UTF-8 BOM，Excel 可直接打开）；</li>
 *     <li>`GET /reports/monthly/employees/{id}`：单个员工当月的订单明细。</li>
 * </ul>
 * 访问控制：员工消费属于财务视角，需要报表/审计权限点，或财务管理、餐厅经理角色。
 */
@Controller
@RequestMapping("/reports/monthly")
@RequiresPerm(value = {PermissionEnum.REPORT_VIEW, PermissionEnum.AUDIT_VIEW},
		roles = {RoleCodes.MANAGER, RoleCodes.FINANCE})
public class MonthlyConsumptionPageController
{
	/**
	 * CSV 表头：与页面表格列保持一致
	 */
	private static final List<String> CSV_HEADER = List.of(
			"工号", "姓名", "部门", "工位", "就餐天数", "订单数", "菜品份数", "消费金额（元）");

	private final ConsumptionAuditService consumptionAuditService;

	/**
	 * 构造器注入
	 *
	 * @param consumptionAuditService 员工消费审计服务
	 */
	public MonthlyConsumptionPageController(ConsumptionAuditService consumptionAuditService) {
		this.consumptionAuditService = consumptionAuditService;
	}

	/**
	 * 员工月度消费报表：一个月一行一个人的消费汇总
	 *
	 * @param month 月份（`yyyy-MM`），为空时取当月
	 * @param model 视图模型
	 * @return 视图名
	 */
	@GetMapping
	public String page(@RequestParam(value = "month", required = false) String month, Model model) {
		YearMonth target = resolveMonth(month);
		List<EmployeeMonthlyConsumptionVO> rows = consumptionAuditService.sumByEmployee(target, null);
		long orderCount = 0L;
		long totalQuantity = 0L;
		BigDecimal totalAmount = BigDecimal.ZERO;
		for (EmployeeMonthlyConsumptionVO row : rows) {
			orderCount += row.getOrderCount() == null ? 0L : row.getOrderCount();
			totalQuantity += row.getTotalQuantity() == null ? 0L : row.getTotalQuantity();
			totalAmount = totalAmount.add(row.getTotalAmount() == null ? BigDecimal.ZERO : row.getTotalAmount());
		}
		model.addAttribute("month", MonthConverter.format(target));
		model.addAttribute("monthText", monthText(target));
		model.addAttribute("rows", rows);
		model.addAttribute("employeeCount", rows.size());
		model.addAttribute("orderCount", orderCount);
		model.addAttribute("totalQuantity", totalQuantity);
		model.addAttribute("totalAmount", totalAmount);
		return "report/monthly-consumption";
	}

	/**
	 * 导出员工月度消费报表（CSV）
	 * <p>
	 * 文件流接口成功时不包 {@code Result}，由浏览器按附件下载；文件名同时给 ASCII 与
	 * UTF-8 两种写法，兼顾旧浏览器。
	 *
	 * @param month 月份（`yyyy-MM`），为空时取当月
	 * @return CSV 文件
	 */
	@GetMapping("/export")
	public ResponseEntity<Resource> export(@RequestParam(value = "month", required = false) String month) {
		YearMonth target = resolveMonth(month);
		List<EmployeeMonthlyConsumptionVO> rows = consumptionAuditService.sumByEmployee(target, null);
		List<List<String>> data = new ArrayList<>();
		long orderCount = 0L;
		long totalQuantity = 0L;
		BigDecimal totalAmount = BigDecimal.ZERO;
		for (EmployeeMonthlyConsumptionVO row : rows) {
			long rowOrders = row.getOrderCount() == null ? 0L : row.getOrderCount();
			long rowQuantity = row.getTotalQuantity() == null ? 0L : row.getTotalQuantity();
			BigDecimal rowAmount = row.getTotalAmount() == null ? BigDecimal.ZERO : row.getTotalAmount();
			orderCount += rowOrders;
			totalQuantity += rowQuantity;
			totalAmount = totalAmount.add(rowAmount);
			data.add(List.of(
					text(row.getEmployeeNo()),
					text(row.getEmployeeName()),
					text(row.getDeptName()),
					text(row.getWorkstation()),
					String.valueOf(row.getActiveDays() == null ? 0L : row.getActiveDays()),
					String.valueOf(rowOrders),
					String.valueOf(rowQuantity),
					amount(rowAmount)));
		}
		// 末行合计：员工人数与三项消费指标，直接从明细行累加，避免与页面的汇总口径不一致
		data.add(List.of("合计", "共 " + rows.size() + " 人", "", "",
				"—", String.valueOf(orderCount), String.valueOf(totalQuantity), amount(totalAmount)));
		Resource resource = TabularExport.csv(CSV_HEADER, data);
		String monthLabel = MonthConverter.format(target);
		String fileName = URLEncoder.encode(monthLabel + "-员工月度消费报表.csv", StandardCharsets.UTF_8)
				.replace("+", "%20");
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
						+ monthLabel + "-employee-consumption.csv\"; filename*=UTF-8''" + fileName)
				.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
				.body(resource);
	}

	/**
	 * 单个员工的当月订单明细（从汇总表下钻）
	 *
	 * @param employeeId 员工ID
	 * @param month      月份（`yyyy-MM`），为空时取当月
	 * @param model      视图模型
	 * @return 视图名
	 */
	@GetMapping("/employees/{employeeId}")
	public String detail(
			@PathVariable("employeeId") Long employeeId,
			@RequestParam(value = "month", required = false) String month,
			Model model) {
		YearMonth target = resolveMonth(month);
		EmployeeConsumptionVO detail = consumptionAuditService.auditEmployee(employeeId, target, true);
		model.addAttribute("month", MonthConverter.format(target));
		model.addAttribute("monthText", monthText(target));
		model.addAttribute("data", detail);
		// 明细条数由服务层截断（默认最多 100 张），超出时页面要提示“去订单模块查全部”
		model.addAttribute("truncated", detail.getOrderCount() != null
				&& detail.getDetails() != null
				&& detail.getOrderCount() > detail.getDetails().size());
		return "report/employee-consumption";
	}

	// ------------------------------------------------------------------ 内部方法

	/**
	 * 解析月份：缺省取当月，非法格式由 {@link MonthConverter} 统一转 40001
	 *
	 * @param month 月份文本
	 * @return 月份
	 */
	private YearMonth resolveMonth(String month) {
		return month == null || month.isBlank() ? YearMonth.now() : MonthConverter.parse(month);
	}

	/**
	 * 页面用中文月份文案
	 *
	 * @param month 月份
	 * @return 形如“2026 年 11 月”
	 */
	private String monthText(YearMonth month) {
		return month.getYear() + " 年 " + month.getMonthValue() + " 月";
	}

	/**
	 * CSV 单元格：空值写空串，避免出现 "null"
	 *
	 * @param value 原值
	 * @return 单元格文本
	 */
	private String text(String value) {
		return value == null || value.isBlank() ? "" : value;
	}

	/**
	 * CSV 金额：固定两位小数，Excel 打开后可直接求和
	 *
	 * @param value 金额
	 * @return 单元格文本
	 */
	private String amount(BigDecimal value) {
		BigDecimal safe = value == null ? BigDecimal.ZERO : value;
		return safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}
}
