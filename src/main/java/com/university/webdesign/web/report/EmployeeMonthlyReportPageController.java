package com.university.webdesign.web.report;

import com.university.webdesign.api.user.view.EmployeeMonthlyReportPageView;
import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.service.order.OrderStatisticsService;
import com.university.webdesign.service.order.dto.MonthlySummaryVO;
import com.university.webdesign.service.report.ConsumptionAuditService;
import com.university.webdesign.service.report.dto.EmployeeConsumptionVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;
import java.util.Objects;

/**
 * 员工月度消费与订单汇总报表页面。
 * <p>
 * 页面只负责取数与组装视图；本人/经理/财务的访问边界、订单状态过滤与快照口径
 * 全部由 {@link OrderStatisticsService} 和 {@link ConsumptionAuditService} 判定。
 */
@Controller
@RequestMapping("/reports")
public class EmployeeMonthlyReportPageController
{
	private final OrderStatisticsService orderStatisticsService;
	private final ConsumptionAuditService consumptionAuditService;

	/**
	 * 构造器注入
	 *
	 * @param orderStatisticsService 个人月度统计服务
	 * @param consumptionAuditService 员工消费审计服务
	 */
	public EmployeeMonthlyReportPageController(
			OrderStatisticsService orderStatisticsService,
			ConsumptionAuditService consumptionAuditService) {
		this.orderStatisticsService = orderStatisticsService;
		this.consumptionAuditService = consumptionAuditService;
	}

	/**
	 * 查看员工月度消费与订单汇总
	 *
	 * @param employeeId 目标员工ID；为空表示当前登录员工
	 * @param month      月份（yyyy-MM）；为空表示当月
	 * @param model      视图模型
	 * @return 报表视图
	 */
	@GetMapping("/employee-monthly")
	public String employeeMonthly(
			@RequestParam(value = "employeeId", required = false) Long employeeId,
			@RequestParam(value = "month", required = false) String month,
			Model model) {
		UserContext context = UserContextHolder.require();
		YearMonth targetMonth = parseMonth(month);
		Long targetEmployeeId = employeeId == null ? context.userId() : employeeId;

		MonthlySummaryVO summary = orderStatisticsService.monthlySummary(targetEmployeeId, targetMonth);
		EmployeeConsumptionVO audit = consumptionAuditService.auditEmployee(
				targetEmployeeId, targetMonth, true);
		summary.setEmployeeName(audit.getEmployeeName());

		EmployeeMonthlyReportPageView view = new EmployeeMonthlyReportPageView();
		view.setSummary(summary);
		view.setAudit(audit);
		view.setSelfView(Objects.equals(context.userId(), audit.getEmployeeId()));
		view.setMonthLabel(targetMonth.getYear() + "年" + targetMonth.getMonthValue() + "月");
		model.addAttribute("data", view);
		return "report/employee-monthly";
	}

	/**
	 * 解析月份参数
	 *
	 * @param value yyyy-MM 文本
	 * @return 月份
	 */
	private YearMonth parseMonth(String value) {
		if (value == null || value.isBlank()) {
			return YearMonth.now();
		}
		try {
			return YearMonth.parse(value.trim());
		} catch (RuntimeException exception) {
			throw BusinessException.paramInvalid("月份格式应为 yyyy-MM");
		}
	}
}
