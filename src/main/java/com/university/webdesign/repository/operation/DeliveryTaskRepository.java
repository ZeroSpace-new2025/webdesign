package com.university.webdesign.repository.operation;

import com.university.webdesign.domain.operation.DeliveryTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 配送任务仓储（`delivery_task`）。
 * <p>
 * `(statistics_date, employee_id)` 唯一：生成任务前先用
 * {@link #findByStatisticsDateAndEmployeeIdIn} 查出已有任务并跳过，保证不重复派单，
 * 数据库唯一约束作为并发兜底。
 * <p>
 * 继承 {@link JpaSpecificationExecutor} 以便按 `DeliveryQuery` 组装动态条件
 * （日期 / 部门 / 工位 / 员工 / 状态），让分页与总数在数据库侧完成。
 */
@Repository
public interface DeliveryTaskRepository extends JpaRepository<DeliveryTask, Long>, JpaSpecificationExecutor<DeliveryTask>
{
	/**
	 * 查询某日指定员工集合的既有任务（去重派的依据）
	 *
	 * @param statisticsDate 就餐日期
	 * @param employeeIds    员工ID集合
	 * @return 既有任务列表
	 */
	List<DeliveryTask> findByStatisticsDateAndEmployeeIdIn(LocalDate statisticsDate, Collection<Long> employeeIds);

	/**
	 * 查询某日全部任务（按任务号升序），供台账导出
	 *
	 * @param statisticsDate 就餐日期
	 * @return 任务列表
	 */
	List<DeliveryTask> findByStatisticsDateOrderByTaskNoAsc(LocalDate statisticsDate);

	/**
	 * 统计某日任务数，用于生成任务编号流水
	 *
	 * @param statisticsDate 就餐日期
	 * @return 任务数
	 */
	long countByStatisticsDate(LocalDate statisticsDate);
}
