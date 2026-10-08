package com.university.webdesign.repository.report;

import com.university.webdesign.domain.report.MonthlyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * 月度销售报表（`monthly_report`）数据访问接口。
 * <p>
 * 归属用户与报表中心（M4），只允许 `service/report` 下的实现类使用，
 * 其他模块若需报表数据必须经 `ReportService` / `ConsumptionAuditService` 契约。
 */
public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long>
{
	/**
	 * 按月份查询报表
	 *
	 * @param reportMonth 月份
	 * @return 报表
	 */
	Optional<MonthlyReport> findByReportMonth(YearMonth reportMonth);

	/**
	 * 判断某月报表是否已生成
	 *
	 * @param reportMonth 月份
	 * @return 已存在返回 true
	 */
	boolean existsByReportMonth(YearMonth reportMonth);

	/**
	 * 按月份查询报表并一次性抓取明细（避免 N+1）
	 *
	 * @param reportMonth 月份
	 * @return 含明细的报表
	 */
	@Query("select distinct report from MonthlyReport report left join fetch report.items "
			+ "where report.reportMonth = :reportMonth")
	Optional<MonthlyReport> findWithItemsByReportMonth(@Param("reportMonth") YearMonth reportMonth);

	/**
	 * 按月份区间查询报表（含明细），月份倒序
	 *
	 * @param from 起始月份（含）
	 * @param to   结束月份（含）
	 * @return 报表列表
	 */
	@Query("select distinct report from MonthlyReport report left join fetch report.items "
			+ "where report.reportMonth between :from and :to order by report.reportMonth desc")
	List<MonthlyReport> findAllWithItemsByReportMonthBetween(@Param("from") YearMonth from,
			@Param("to") YearMonth to);

	/**
	 * 删除某月报表，供重算前覆盖写入
	 *
	 * @param reportMonth 月份
	 */
	void deleteByReportMonth(YearMonth reportMonth);
}
