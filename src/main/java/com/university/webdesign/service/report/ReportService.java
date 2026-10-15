package com.university.webdesign.service.report;

import com.university.webdesign.service.order.dto.ExportFormat;
import com.university.webdesign.service.report.dto.MonthlyReportVO;
import org.springframework.core.io.Resource;

import java.time.YearMonth;

/**
 * 财务报表服务。
 * <p>
 * 对应《对外方法表》5.2 的 {@code ReportService}：月度销售报表的生成、查询、导出与刷新。
 * <p>
 * 权限：财务管理（{@code RoleCodes.FINANCE}）或餐厅经理（{@code RoleCodes.MANAGER}），
 * 由实现类校验，越权抛 40300。
 * <p>
 * 取数边界：订单数据一律经
 * {@link com.university.webdesign.service.order.OrderQueryService} 读取，
 * **不得**直接访问 `order_form` / `order_detail` 表，也不得新建订单实体。
 */
public interface ReportService
{
	/**
	 * 生成月度销售报表（M4-21）
	 * <p>
	 * 聚合当月有效订单，按菜品汇总数量与金额并写入 `monthly_report` 缓存表。
	 * {@code force=false} 且当月报表已存在时直接返回既有报表ID（幂等）。
	 *
	 * @param month 月份
	 * @param force 是否强制重算
	 * @return 报表ID
	 */
	Long generateMonthly(YearMonth month, boolean force);

	/**
	 * 查询月度销售报表（M4-22）
	 * <p>
	 * 优先读 `monthly_report` 缓存；缺失则同步生成后返回。
	 * {@code deptId} 非空时只返回该部门员工的菜品销售明细。
	 *
	 * @param month  月份
	 * @param deptId 部门ID，可为空表示全餐厅
	 * @return 月度报表
	 */
	MonthlyReportVO getMonthly(YearMonth month, Long deptId);

	/**
	 * 导出月度报表（M4-23）
	 * <p>
	 * 支持 XLSX（SpreadsheetML 2003 XML，Excel 可直接打开）与 CSV；
	 * PDF 未引入生成库，请求时抛 40001 并提示仅支持 XLSX/CSV。
	 *
	 * @param month 月份
	 * @param fmt   导出格式
	 * @return 文件资源，由 Controller 以附件形式返回
	 */
	Resource exportMonthly(YearMonth month, ExportFormat fmt);

	/**
	 * 刷新月度报表缓存（M4-24）
	 * <p>
	 * 订单变更后重算并覆盖缓存，幂等。
	 *
	 * @param month 月份
	 */
	void refreshMonthly(YearMonth month);

	/**
	 * 查询已有报表的月份列表
	 * <p>
	 * 供前端月份选择器使用（对应《对外方法表》M4-22 的页面配套能力）。
	 * 无任何报表时返回当月，保证选择器不会是空的。
	 *
	 * @return 月份列表，倒序
	 */
	java.util.List<YearMonth> listReportMonths();
}
