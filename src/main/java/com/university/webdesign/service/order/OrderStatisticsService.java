package com.university.webdesign.service.order;

import com.university.webdesign.service.order.dto.ExportFormat;
import com.university.webdesign.service.order.dto.MonthlySummaryVO;
import org.springframework.core.io.Resource;

import java.time.YearMonth;

/**
 * 订单与交易核心——个人统计服务。
 * <p>
 * 对应《对外方法表》3.2 的 `OrderStatisticsService`：个人月度消费统计（M2-09）
 * 与消费明细导出/打印（M2-10）。
 */
public interface OrderStatisticsService
{
	/**
	 * 个人月度消费统计（M2-09）
	 * <p>
	 * 员工只能查自己；经理与财务可查任意员工（消费审计），越权抛 40300。
	 *
	 * @param employeeId 员工ID
	 * @param month      月份
	 * @return 月度汇总（含分类占比与菜品明细）
	 */
	MonthlySummaryVO monthlySummary(Long employeeId, YearMonth month);

	/**
	 * 导出个人消费明细（M2-10）
	 *
	 * @param employeeId 员工ID
	 * @param month      月份
	 * @param format     导出格式
	 * @return 文件资源，由 Controller 以附件形式返回
	 */
	Resource exportHistory(Long employeeId, YearMonth month, ExportFormat format);
}
