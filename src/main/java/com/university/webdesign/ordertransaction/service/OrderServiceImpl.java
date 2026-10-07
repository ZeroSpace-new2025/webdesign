package com.university.webdesign.ordertransaction.service;

import com.university.webdesign.ordertransaction.api.OrderDTO;
import com.university.webdesign.ordertransaction.api.OrderQueryData;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 【临时占位实现】为了让应用在其他模块完成前能够启动而添加。
 * 订单与交易核心（Order & Transaction Service）应由对应负责人实现，
 * 完成后请用真正的实现类替换本类。
 */
@Service
public class OrderServiceImpl implements OrderService {

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
		throw new UnsupportedOperationException("订单服务尚未实现");
	}

	@Override
	public OrderDTO updateOrder(OrderDTO orderDTO) {
		throw new UnsupportedOperationException("订单服务尚未实现");
	}

	@Override
	public void deleteOrder(Long orderId) {
		// 暂不处理
	}
}
