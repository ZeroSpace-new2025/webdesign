package com.university.webdesign.api.user;

import com.university.webdesign.common.Result;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.config.RequiresPerm;
import com.university.webdesign.service.order.dto.ExportFormat;
import com.university.webdesign.service.report.ConsumptionAuditService;
import com.university.webdesign.service.report.MonthConverter;
import com.university.webdesign.service.report.ReportService;
import com.university.webdesign.service.report.dto.DeptConsumptionVO;
import com.university.webdesign.service.report.dto.EmployeeConsumptionVO;
import com.university.webdesign.service.report.dto.MonthlyReportVO;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * 财务报表 REST 控制器（基础路径 {@code /api/v1/user}）。
 * <p>
 * 对应《对外方法表》5.1.3 共 6 个接口：生成/查询/导出/刷新月度报表、
 * 员工消费审计、部门消费汇总。本类只做协议转换（含 {@code month} 文本 → {@code YearMonth}），
 * 业务规则都在 {@link ReportService} / {@link ConsumptionAuditService} 中。
 * <p>
 * 鉴权：由 {@code config.AuthInterceptor} 按 {@link RequiresPerm} 声明统一校验；
 * 报表查看用 {@link PermissionEnum#REPORT_VIEW}，导出额外接受
 * {@link PermissionEnum#REPORT_EXPORT}，消费审计接受 {@link PermissionEnum#AUDIT_VIEW} / 财务角色。
 */
@RestController
@RequestMapping("/api/v1/user")
public class ApiReportController extends ApiUserSupport
{
	private final ReportService reportService;
	private final ConsumptionAuditService consumptionAuditService;

	/**
	 * 构造器注入
	 *
	 * @param reportService           财务报表服务
	 * @param consumptionAuditService 消费审计服务
	 */
	public ApiReportController(ReportService reportService,
			ConsumptionAuditService consumptionAuditService) {
		this.reportService = reportService;
		this.consumptionAuditService = consumptionAuditService;
	}

	/**
	 * M4-21 生成月度销售报表
	 *
	 * @param month 月份（yyyy-MM）
	 * @param force 是否强制重算，默认 false
	 * @return 报表ID与生成时间
	 */
	@PostMapping("/reports/monthly/generate")
	@RequiresPerm(PermissionEnum.REPORT_VIEW)
	public Result<Map<String, Object>> generateMonthly(
			@RequestParam("month") String month,
			@RequestParam(value = "force", defaultValue = "false") boolean force) {
		Long reportId = reportService.generateMonthly(MonthConverter.parse(month), force);
		MonthlyReportVO vo = reportService.getMonthly(MonthConverter.parse(month), null);
		return Result.success(Map.of(
				"reportId", reportId,
				"generatedAt", vo.getGeneratedAt() == null ? "" : vo.getGeneratedAt()));
	}

	/**
	 * M4-22 查询月度销售报表（优先读缓存，缺失则同步生成）
	 *
	 * @param month  月份（yyyy-MM）
	 * @param deptId 部门ID，可选
	 * @return 月度报表
	 */
	@GetMapping("/reports/monthly")
	@RequiresPerm(PermissionEnum.REPORT_VIEW)
	public Result<MonthlyReportVO> getMonthly(
			@RequestParam("month") String month,
			@RequestParam(value = "deptId", required = false) Long deptId) {
		return Result.success(reportService.getMonthly(MonthConverter.parse(month), deptId));
	}

	/**
	 * M4-23 导出月度报表（XLSX / CSV）
	 *
	 * @param month  月份（yyyy-MM）
	 * @param format 导出格式，默认 XLSX
	 * @return 报表文件
	 */
	@GetMapping("/reports/monthly/export")
	@RequiresPerm({PermissionEnum.REPORT_VIEW, PermissionEnum.REPORT_EXPORT})
	public ResponseEntity<Resource> exportMonthly(
			@RequestParam("month") String month,
			@RequestParam(value = "format", required = false) String format) {
		ExportFormat exportFormat = ExportFormat.parse(format);
		Resource resource = reportService.exportMonthly(MonthConverter.parse(month), exportFormat);
		if (exportFormat == ExportFormat.CSV) {
			String fileName = URLEncoder.encode(month + "-月度销售报表.csv", StandardCharsets.UTF_8)
					.replace("+", "%20");
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
							+ month + "-monthly-report.csv\"; filename*=UTF-8''" + fileName)
					.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
					.body(resource);
		}
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
						+ month + "-monthly-report.xls\"")
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(resource);
	}

	/**
	 * M4-24 刷新月度报表缓存（幂等重算）
	 *
	 * @param month 月份（yyyy-MM）
	 * @return 报表ID与刷新时间
	 */
	@PostMapping("/reports/monthly/refresh")
	@RequiresPerm(PermissionEnum.REPORT_VIEW)
	public Result<Map<String, Object>> refreshMonthly(@RequestParam("month") String month) {
		reportService.refreshMonthly(MonthConverter.parse(month));
		MonthlyReportVO vo = reportService.getMonthly(MonthConverter.parse(month), null);
		return Result.success(Map.of(
				"reportId", vo.getReportId() == null ? 0L : vo.getReportId(),
				"refreshedAt", vo.getGeneratedAt() == null ? "" : vo.getGeneratedAt()));
	}

	/**
	 * 查询已有报表的月份列表（前端月份选择器的数据来源）
	 *
	 * @return 月份列表（yyyy-MM），倒序；无报表时返回当月
	 */
	@GetMapping("/reports/months")
	@RequiresPerm({PermissionEnum.REPORT_VIEW, PermissionEnum.AUDIT_VIEW})
	public Result<List<String>> reportMonths() {
		return Result.success(reportService.listReportMonths().stream()
				.map(YearMonth::toString)
				.toList());
	}

	/**
	 * M4-25 员工消费审计
	 *
	 * @param employeeId  员工ID
	 * @param month       月份（yyyy-MM）
	 * @param withDetails 是否返回逐单明细，默认 true
	 * @return 员工月度消费汇总
	 */
	@GetMapping("/reports/employee-consumption")
	@RequiresPerm(value = PermissionEnum.AUDIT_VIEW, roles = RoleCodes.FINANCE)
	public Result<EmployeeConsumptionVO> employeeConsumption(
			@RequestParam("employeeId") Long employeeId,
			@RequestParam("month") String month,
			@RequestParam(value = "withDetails", defaultValue = "true") boolean withDetails) {
		return Result.success(consumptionAuditService.auditEmployee(
				employeeId, MonthConverter.parse(month), withDetails));
	}

	/**
	 * M4-26 部门消费汇总
	 *
	 * @param month   月份（yyyy-MM）
	 * @param deptIds 部门ID集合，可选（为空表示全部部门）
	 * @return 部门维度汇总
	 */
	@GetMapping("/reports/dept-consumption")
	@RequiresPerm(value = PermissionEnum.AUDIT_VIEW, roles = RoleCodes.FINANCE)
	public Result<List<DeptConsumptionVO>> deptConsumption(
			@RequestParam("month") String month,
			@RequestParam(value = "deptIds", required = false) List<Long> deptIds) {
		return Result.success(consumptionAuditService.sumByDept(MonthConverter.parse(month), deptIds));
	}
}
