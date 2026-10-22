package com.university.webdesign.ordertransaction.service;

import com.university.webdesign.ordertransaction.api.OrderDTO;
import com.university.webdesign.ordertransaction.api.OrderQueryData;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单模块启动占位实现，本次不改动其业务逻辑。
 */
@Service
public class DefaultOrderService implements OrderService
{
	@Override
	public List<OrderDTO> query(OrderQueryData queryData) {
		return List.of();
	}

	@Override
	public List<OrderDTO> getTodayOrders() {
		return List.of();
	}

	@Override
	public OrderDTO createOrder(Long userid) {
		return new OrderDTO();
	}

	@Override
	public OrderDTO updateOrder(OrderDTO orderDTO) {
		return orderDTO;
	}

	@Override
	public void deleteOrder(Long orderId) {
		// 占位实现。
	}
}
