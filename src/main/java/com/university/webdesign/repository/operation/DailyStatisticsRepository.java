package com.university.webdesign.repository.operation;

import com.university.webdesign.domain.operation.DailyStatistics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 每日汇总快照仓储（`daily_statistics`）。
 * <p>
 * `statistics_date` 唯一：聚合与刷新都按日期定位同一条快照，先删明细后覆盖写，保证幂等。
 */
@Repository
public interface DailyStatisticsRepository extends JpaRepository<DailyStatistics, Long>
{
	/**
	 * 按汇总日期查询快照
	 *
	 * @param statisticsDate 汇总日期
	 * @return 快照
	 */
	Optional<DailyStatistics> findByStatisticsDate(LocalDate statisticsDate);

	/**
	 * 判断某日快照是否已存在
	 *
	 * @param statisticsDate 汇总日期
	 * @return 已存在时返回 true
	 */
	boolean existsByStatisticsDate(LocalDate statisticsDate);

	/**
	 * 按日期区间分页查询快照（按日期倒序）
	 *
	 * @param from     起始日期（含）
	 * @param to       结束日期（含）
	 * @param pageable 分页参数
	 * @return 分页快照
	 */
	Page<DailyStatistics> findByStatisticsDateBetweenOrderByStatisticsDateDesc(
			LocalDate from, LocalDate to, Pageable pageable);

	/**
	 * 按日期区间分页查询快照（不限边界）
	 *
	 * @param pageable 分页参数
	 * @return 分页快照
	 */
	Page<DailyStatistics> findAllByOrderByStatisticsDateDesc(Pageable pageable);
}
