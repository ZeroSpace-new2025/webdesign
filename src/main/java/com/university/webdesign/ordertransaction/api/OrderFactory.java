package com.university.webdesign.ordertransaction.api;

import com.university.webdesign.ordertransaction.impl.*;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("UnusedDeclaration")
public class OrderFactory
{
	public static OrderApi createOrder()
	{
		//todo 这里的参数可以根据实际情况进行修改
		return new OrderImpl(0, 0, System.currentTimeMillis());
	}
	
	/**
	 * 开始一个新的订单搜索
	 *
	 * @return OrderSearchApi 实例
	 */
	OrderSearchApi startOrderSearch()
	{
		//todo 这里的参数可以根据实际情况进行修改
		return new OrderSearchImpl();
	}
}
