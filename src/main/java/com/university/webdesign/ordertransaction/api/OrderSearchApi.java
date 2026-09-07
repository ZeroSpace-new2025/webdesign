package com.university.webdesign.ordertransaction.api;

import org.jspecify.annotations.NonNull;

import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public interface OrderSearchApi
{
	
	/**
	 * 添加用户搜索条件
	 *
	 * @param userId 用户 ID
	 * @return OrderSearchApi 实例
	 */
	OrderSearchApi addUserSearchCondition(long userId);
	
	/**
	 * 添加时间范围搜索条件
	 *
	 * @param startTime 开始时间
	 * @param endTime 结束时间
	 * @return OrderSearchApi 实例
	 * @remark 时间范围搜索条件是指搜索在指定时间范围内的订单，时间范围是闭区间，即包含开始时间和结束时间
	 * @remark 新条件会覆盖之前的条件
	 */
	OrderSearchApi addTimeRangeSearchCondition(long startTime, long endTime);
	
	/**
	 * 移除用户搜索条件
	 *
	 * @param userId 用户 ID
	 * @return OrderSearchApi 实例
	 */
	OrderSearchApi removeUserSearchCondition(long userId);
	
	/**
	 * 移除时间范围搜索条件
	 *
	 * @return OrderSearchApi 实例
	 */
	OrderSearchApi removeTimeRangeSearchCondition();
	/**
	 * 执行搜索
	 *
	 * @return 订单数据列表
	 * @remark 数据库操作
	 */
	@NonNull
	List<OrderData> executeSearch();
}
