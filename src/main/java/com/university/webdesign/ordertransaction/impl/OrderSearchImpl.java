package com.university.webdesign.ordertransaction.impl;

import com.university.webdesign.ordertransaction.api.OrderData;
import com.university.webdesign.ordertransaction.api.OrderSearchApi;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class OrderSearchImpl implements OrderSearchApi
{
	@Override
	public OrderSearchApi addUserSearchCondition(long userId)
	{
		return this;
	}
	
	@Override
	public OrderSearchApi addTimeRangeSearchCondition(long startTime, long endTime)
	{
		return this;
	}
	
	@Override
	public OrderSearchApi removeUserSearchCondition(long userId)
	{
		return this;
	}
	
	@Override
	public OrderSearchApi removeTimeRangeSearchCondition()
	{
		return this;
	}
	
	@Override
	public @NonNull List<OrderData> executeSearch()
	{
		//todo 这里需要实现搜索逻辑，返回符合条件的订单数据列表
		return List.of();
	}
}
