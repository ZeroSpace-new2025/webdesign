package com.university.webdesign.api.user.view;

import com.university.webdesign.service.order.dto.MonthlySummaryVO;
import com.university.webdesign.service.report.dto.EmployeeConsumptionVO;
import lombok.Data;

/**
 * 员工月度消费与订单汇总页面视图。
 * <p>
 * 员工本人、餐厅经理与财务共用同一报表页：员工只能查看自己，经理/财务可查看指定员工。
 */
@Data
public class EmployeeMonthlyReportPageView
{
	/**
	 * 月度消费统计（分类、菜品汇总）
	 */
	private MonthlySummaryVO summary;

	/**
	 * 员工月度订单明细
	 */
	private EmployeeConsumptionVO audit;

	/**
	 * 当前查看的是否为本人报表
	 */
	private boolean selfView;

	/**
	 * 月份中文展示文案
	 */
	private String monthLabel;
}
