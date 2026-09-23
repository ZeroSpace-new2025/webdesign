package com.university.webdesign.ordertransaction.api;

import com.university.webdesign.common.Result;
import com.university.webdesign.ordertransaction.service.OrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/*其他的API接口可以根据需要添加。修改后要立即提交。*/
/*应该只包含与订单相关的API接口，而不应该包含其他相关的接口。*/
/*请勿把内部实现暴露给外部。*/

/**
 * 订单 API
 * <p>
 * 本类只做参数接收与结果包装，所有业务规则（时间窗口、一人一天一单、快照等）都在
 * {@link OrderService} 中实现。
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
	public Result<List<OrderDTO>> queryOrders(@RequestBody OrderQueryData queryData) {
		var orders = orderService.query(queryData);
		return Result.success(orders);
	}
	
	/**
	 * 个人历史订单查询
	 *
	 * @param userId 员工ID
	 * @param start  起始日期（含），可选
	 * @param end    结束日期（含），可选
	 * @return 历史订单列表
	 */
	@GetMapping("/history")
	public Result<List<OrderDTO>> getHistoryOrders(
			@RequestParam("userId") Long userId,
			@RequestParam(value = "start", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(value = "end", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
		return Result.success(orderService.getHistoryOrders(userId, start, end));
	}
	
	/**
	 * 个人月度消费统计（供员工查看/打印，也供财务与经理查询特定员工消费）
	 *
	 * @param userId 员工ID
	 * @param year   年份
	 * @param month  月份（1-12）
	 * @return 月度消费统计
	 */
	@GetMapping("/consumption/monthly")
	public Result<PersonalConsumptionDTO> getMonthlyConsumption(
			@RequestParam("userId") Long userId,
			@RequestParam("year") int year,
			@RequestParam("month") int month) {
		return Result.success(orderService.getMonthlyConsumption(userId, year, month));
	}
	
	/**
	 * 按订单ID查询订单
	 *
	 * @param id 订单ID
	 * @return 订单
	 */
	@GetMapping("/{id}")
	public Result<OrderDTO> getOrder(@PathVariable("id") Long id) {
		OrderDTO order = orderService.getOrder(id);
		if (order == null) {
			return Result.failure(404, "订单不存在");
		}
		return Result.success(order);
	}
	
	/**
	 * 创建订单
	 *
	 * @param createData 下单请求（员工ID + 菜品与数量）
	 * @return 创建的订单
	 */
	@PostMapping("/create")
	public Result<OrderDTO> createOrder(@RequestBody OrderCreateData createData) {
		var order = orderService.createOrder(createData);
		return Result.success(order);
	}
	
	/**
	 * 更新订单（员工在截止时间前修改自己订单的菜品与数量）
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
	 * 更新订单（改单请求体形式）
	 *
	 * @param updateData 改单请求
	 * @return 更新后的订单
	 */
	@PutMapping("/update")
	public Result<OrderDTO> updateOrder(@RequestBody OrderUpdateData updateData) {
		var order = orderService.updateOrder(updateData);
		return Result.success(order);
	}
	
	/**
	 * 取消订单（员工在截止时间前取消自己的订单）
	 *
	 * @param id         订单ID
	 * @param operatorId 操作用户ID
	 * @return 取消后的订单
	 */
	@PostMapping("/{id}/cancel")
	public Result<OrderDTO> cancelOrder(@PathVariable("id") Long id,
			@RequestParam(value = "operatorId", required = false) Long operatorId) {
		return Result.success(orderService.cancelOrder(id, operatorId));
	}
	
	/**
	 * 订单支付
	 *
	 * @param id         订单ID
	 * @param operatorId 操作用户ID
	 * @return 支付后的订单
	 */
	@PostMapping("/{id}/pay")
	public Result<OrderDTO> payOrder(@PathVariable("id") Long id,
			@RequestParam(value = "operatorId", required = false) Long operatorId) {
		return Result.success(orderService.payOrder(id, operatorId));
	}
	
	/**
	 * 删除订单（经理删除违规订单）
	 *
	 * @param id         订单ID
	 * @param operatorId 操作经理ID
	 * @return 删除后的订单
	 */
	@DeleteMapping("/{id}")
	public Result<OrderDTO> deleteOrder(@PathVariable("id") Long id,
			@RequestParam(value = "operatorId", required = false) Long operatorId) {
		return Result.success(orderService.deleteOrder(id, operatorId));
	}
	
	/**
	 * 业务异常统一转为失败结果，避免把 500 抛给前端
	 * <p>
	 * //todo 确认：全局异常处理是否统一放到 {@code common} 包的 {@code @RestControllerAdvice}
	 * 中（其他模块也会用），确认后本方法应迁出并在 common 中统一实现。
	 *
	 * @param e 异常
	 * @return 失败结果
	 */
	@ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
	public Result<Void> handleBusinessException(RuntimeException e) {
		return Result.failure(HttpStatus.BAD_REQUEST.value(), e.getMessage());
	}
}
