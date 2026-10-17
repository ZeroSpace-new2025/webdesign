package com.university.webdesign.ordertransaction.repository;

import com.university.webdesign.ordertransaction.data.Order;
import com.university.webdesign.ordertransaction.data.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单仓储
 * <p>
 * 继承 {@link JpaSpecificationExecutor} 以便按 {@code OrderQueryData} 组装动态查询条件。
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order>
{
	/**
	 * 查询某员工在指定时间区间内、指定状态的订单
	 * <p>
	 * 用于“一人一天一单”校验：传入当天 [00:00, 次日 00:00) 与有效状态即可。
	 *
	 * @param userId 员工ID
	 * @param start  区间起点（含）
	 * @param end    区间终点（不含）
	 * @param status 订单状态
	 * @return 匹配的订单列表
	 */
	List<Order> findByUserIdAndStatusAndOrderDateGreaterThanEqualAndOrderDateLessThan(
			Long userId, OrderStatus status, LocalDateTime start, LocalDateTime end);
	
	/**
	 * 查询某员工在指定时间区间内的订单（不分状态）
	 *
	 * @param userId 员工ID
	 * @param start  区间起点（含）
	 * @param end    区间终点（不含）
	 * @return 按下单时间升序的订单列表
	 */
	List<Order> findByUserIdAndOrderDateGreaterThanEqualAndOrderDateLessThanOrderByOrderDateAsc(
			Long userId, LocalDateTime start, LocalDateTime end);
	
	/**
	 * 查询指定时间区间内的所有订单（不分状态）
	 *
	 * @param start 区间起点（含）
	 * @param end   区间终点（不含）
	 * @return 按下单时间升序的订单列表
	 */
	List<Order> findByOrderDateGreaterThanEqualAndOrderDateLessThanOrderByOrderDateAsc(
			LocalDateTime start, LocalDateTime end);
	
	/**
	 * 统计某天已生成的订单数量，用于生成当日订单流水号
	 *
	 * @param start 当天起点（含）
	 * @param end   次日起点（不含）
	 * @return 订单数量
	 */
	long countByOrderDateGreaterThanEqualAndOrderDateLessThan(LocalDateTime start, LocalDateTime end);
	
	/**
	 * 按订单号查询订单
	 *
	 * @param orderNumber 订单号
	 * @return 订单
	 */
	Optional<Order> findByOrderNumber(Long orderNumber);
	
	/**
	 * 统计某员工在指定时间区间内的有效订单金额合计
	 * <p>
	 * 只统计非取消状态，供报表/海报类场景直接取合计金额使用。
	 *
	 * @param userId 员工ID
	 * @param start  区间起点（含）
	 * @param end    区间终点（不含）
	 * @return 金额合计，无数据时返回 null
	 */
	@Query("select sum(o.total) from Order o "
			+ "where o.userId = :userId "
			+ "and o.status <> com.university.webdesign.ordertransaction.data.OrderStatus.CANCELLED "
			+ "and o.orderDate >= :start and o.orderDate < :end")
	BigDecimal sumTotalByUserIdAndPeriod(
			@Param("userId") Long userId,
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);
}
