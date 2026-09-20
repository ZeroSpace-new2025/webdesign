package com.university.webdesign.ordertransaction.service;

import com.university.webdesign.ordertransaction.api.OrderDTO;
import com.university.webdesign.ordertransaction.api.OrderQueryData;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface OrderService
{
	List<OrderDTO> query(OrderQueryData queryData);
	List<OrderDTO> getTodayOrders();
	OrderDTO createOrder(Long userid);
	OrderDTO updateOrder(OrderDTO orderDTO);
	void deleteOrder(Long orderId);
}
