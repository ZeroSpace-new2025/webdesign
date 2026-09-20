package com.university.webdesign.ordertransaction.api;

import com.university.webdesign.common.Result;
import com.university.webdesign.ordertransaction.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/*其他的API接口可以根据需要添加。修改后要立即提交。*/
/*应该只包含与订单相关的API接口，而不应该包含其他相关的接口。*/
/*请勿把内部实现暴露给外部。*/

/**
 * 订单 API
 */
@RestController
@RequestMapping("/api/order")
public class OrderApi
{
	private final OrderService orderService;
	
	public OrderApi(OrderService orderService) {
		this.orderService = orderService;
	}
	
	/**
	 * 获取今天的订单列表
	 *
	 * @return 今天的订单列表
	 */
	@GetMapping("/today")
	public Result<List<OrderDTO>> getTodayOrders() {
		var orders = orderService.getTodayOrders();
		return Result.success(orders);

	}
	
	/**
	 * 查询订单列表
	 *
	 * @param queryData 查询条件
	 * @return 订单列表
	 */
	@PostMapping("/query")
	public Result<List<OrderDTO>> queryOrders(OrderQueryData queryData) {
		var orders = orderService.query(queryData);
		return Result.success(orders);
	}
	
	/**
	 * 创建订单
	 *
	 * @return 创建的订单
	 */
	@PostMapping("/create")
	public Result<OrderDTO> createOrder() {
		Long userid = 1L; //todo: get the user id from the request or session
		var order = orderService.createOrder(userid);
		return Result.success(order);
	}
	
	/**
	 * 更新订单
	 *
	 * @param orderDTO 订单DTO
	 * @return 更新后的订单
	 */
	@PutMapping
	public Result<OrderDTO> updateOrder(@RequestBody OrderDTO orderDTO) {
		var order = orderService.updateOrder(orderDTO);
		return Result.success(order);
	}
	
	/**
	 * 删除订单
	 *
	 * @param orderId 订单ID
	 * @return 删除结果
	 */
	@DeleteMapping("/{id}")
	public Result<Void> deleteOrder(@PathVariable("id") Long orderId) {
		orderService.deleteOrder(orderId);
		return Result.success(null);
	}
}
