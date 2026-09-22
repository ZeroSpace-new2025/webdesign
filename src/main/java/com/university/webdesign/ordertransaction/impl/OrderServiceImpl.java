package com.university.webdesign.ordertransaction.impl;

import com.university.webdesign.ordertransaction.api.OrderDTO;
import com.university.webdesign.ordertransaction.api.OrderQueryData;
import com.university.webdesign.ordertransaction.service.OrderService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderServiceImpl implements OrderService
{
	@Override
	public OrderDTO createOrder(Long userId)
	{
		// Implementation of order creation logic
		return null;
	}
	
	@Override
	public OrderDTO updateOrder(OrderDTO orderDTO) {
		// Implementation of order update logic
		return null;
	}
	
	@Override
	public void deleteOrder(Long orderId) {
		// Implementation of order deletion logic
	}
	
	@Override
	public List<OrderDTO> query(OrderQueryData queryData) {
		// Implementation of order query logic
		return null;
	}
	
	@Override
	public List<OrderDTO> getTodayOrders() {
		// Implementation of logic to get today's orders
		return null;
	}
}
