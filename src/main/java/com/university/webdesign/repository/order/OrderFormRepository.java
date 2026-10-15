package com.university.webdesign.repository.order;

import com.university.webdesign.domain.order.OrderForm;
import com.university.webdesign.domain.order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 订单主表仓储。
 * <p>
 * 继承 {@link JpaSpecificationExecutor} 以便按 {@code OrderQuery} 组装动态条件。
 * 所有查询都带 `orderDate` 或 `employeeId` 条件，避免全表扫描（对应 `(employee_id, order_date)` 索引）。
 */
@Repository
public interface OrderFormRepository extends JpaRepository<OrderForm, Long>, JpaSpecificationExecutor<OrderForm>
{
	/**
	 * 按订单号查询
	 *
	 * @param orderNo 订单号
	 * @return 订单
	 */
	Optional<OrderForm> findByOrderNo(String orderNo);

	/**
	 * 按幂等键查询（幂等下单：命中则直接返回既有订单）
	 *
	 * @param idempotencyKey 幂等键
	 * @return 订单
	 */
	Optional<OrderForm> findByIdempotencyKey(String idempotencyKey);

	/**
	 * 查询某员工某日的订单（不分状态）
	 *
	 * @param employeeId 员工ID
	 * @param orderDate  就餐日期
	 * @return 订单列表
	 */
	List<OrderForm> findByEmployeeIdAndOrderDateOrderByCreatedAtAsc(Long employeeId, LocalDate orderDate);

	/**
	 * 查询某员工某日指定状态的订单，用于“一人一天一单”校验
	 *
	 * @param employeeId 员工ID
	 * @param orderDate  就餐日期
	 * @param statuses   状态集合
	 * @return 订单列表
	 */
	List<OrderForm> findByEmployeeIdAndOrderDateAndStatusIn(
			Long employeeId, LocalDate orderDate, Collection<OrderStatus> statuses);

	/**
	 * 查询某员工某日有效订单数量
	 *
	 * @param employeeId 员工ID
	 * @param orderDate  就餐日期
	 * @param statuses   有效状态集合
	 * @return 数量
	 */
	long countByEmployeeIdAndOrderDateAndStatusIn(
			Long employeeId, LocalDate orderDate, Collection<OrderStatus> statuses);

	/**
	 * 查询某日指定状态的订单（供 M3 聚合与配送取数）
	 *
	 * @param orderDate 就餐日期
	 * @param statuses  状态集合
	 * @return 按下单时间升序的订单列表
	 */
	List<OrderForm> findByOrderDateAndStatusInOrderByCreatedAtAsc(LocalDate orderDate, Collection<OrderStatus> statuses);

	/**
	 * 查询某日全部订单（不分状态）
	 *
	 * @param orderDate 就餐日期
	 * @return 按下单时间升序的订单列表
	 */
	List<OrderForm> findByOrderDateOrderByCreatedAtAsc(LocalDate orderDate);

	/**
	 * 查询时间区间内某员工的订单（个人历史与月度统计）
	 *
	 * @param employeeId 员工ID
	 * @param start      起点（含）
	 * @param end        终点（不含）
	 * @return 按下单时间升序的订单列表
	 */
	List<OrderForm> findByEmployeeIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
			Long employeeId, LocalDateTime start, LocalDateTime end);

	/**
	 * 查询时间区间内的全部订单（报表）
	 *
	 * @param start 起点（含）
	 * @param end   终点（不含）
	 * @return 按下单时间升序的订单列表
	 */
	List<OrderForm> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
			LocalDateTime start, LocalDateTime end);

	/**
	 * 统计某日已生成的订单数，用于生成当日流水号
	 *
	 * @param orderDate 就餐日期
	 * @return 订单数
	 */
	long countByOrderDate(LocalDate orderDate);

	/**
	 * 取某日已使用的最大订单号，用于发号（`order_no` 前缀为 `yyyyMMdd`）
	 *
	 * @param prefix 订单号前缀（`yyyyMMdd`）
	 * @return 最大订单号，无数据时返回 null
	 */
	@Query("select max(o.orderNo) from OrderForm o where o.orderNo like concat(:prefix, '%')")
	String findMaxOrderNoByPrefix(@Param("prefix") String prefix);

	/**
	 * 统计某员工某时间区间内的有效订单金额合计（月度消费校验用）
	 *
	 * @param employeeId 员工ID
	 * @param start      起点（含）
	 * @param end        终点（不含）
	 * @return 合计金额，无数据时返回 null
	 */
	@Query("select sum(o.totalAmount) from OrderForm o "
			+ "where o.employeeId = :employeeId and o.status in ('PENDING', 'VALID') "
			+ "and o.createdAt >= :start and o.createdAt < :end")
	BigDecimal sumAmountByEmployeeAndPeriod(
			@Param("employeeId") Long employeeId,
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);
}
