package com.university.webdesign.api.order;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.Result;
import com.university.webdesign.service.order.OrderQueryService;
import com.university.webdesign.service.order.OrderService;
import com.university.webdesign.service.order.dto.OrderModifyCmd;
import com.university.webdesign.service.order.dto.OrderQuery;
import com.university.webdesign.service.order.dto.OrderSubmitCmd;
import com.university.webdesign.service.order.dto.OrderVO;
import com.university.webdesign.service.order.dto.ServiceWindowVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 订单与交易核心 REST 控制器。
 * <p>
 * 对应《对外方法表》M2 的 `ApiOrderController`（基础路径 `/api/v1/order`），
 * 只做协议转换与参数校验，**所有业务规则在 {@link OrderService} 中**。
 * 操作人身份来自认证拦截器写入的登录上下文，不再接受客户端自报的 `operatorId`。
 */
@RestController
@RequestMapping("/api/v1/order")
public class ApiOrderController
{
	private final OrderService orderService;
	private final OrderQueryService orderQueryService;

	public ApiOrderController(OrderService orderService, OrderQueryService orderQueryService) {
		this.orderService = orderService;
		this.orderQueryService = orderQueryService;
	}

	/**
	 * M2-01 查询订餐时间窗口
	 *
	 * @param date 就餐日期，默认当天
	 * @return 时间窗口
	 */
	@GetMapping("/windows")
	public Result<ServiceWindowVO> window(
			@RequestParam(value = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return Result.success(orderService.getWindow(date));
	}

	/**
	 * M2-02 提交订单
	 *
	 * @param idempotencyKey 幂等键，必填
	 * @param cmd            下单请求
	 * @return 创建后的订单
	 */
	@PostMapping("/orders")
	public Result<OrderVO> submit(
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody OrderSubmitCmd cmd) {
		cmd.setIdempotencyKey(idempotencyKey);
		return Result.success(orderService.submit(cmd));
	}

	/**
	 * M2-03 修改订单
	 *
	 * @param orderId 订单ID
	 * @param cmd     改单请求
	 * @return 修改后的订单
	 */
	@PutMapping("/orders/{orderId}")
	public Result<OrderVO> modify(@PathVariable("orderId") Long orderId,
			@Valid @RequestBody OrderModifyCmd cmd) {
		orderService.modify(orderId, cmd);
		return Result.success(orderQueryService.getDetail(orderId));
	}

	/**
	 * M2-06 查询订单详情（仅本人或经理/财务）
	 *
	 * @param orderId 订单ID
	 * @return 订单详情（含快照明细）
	 */
	@GetMapping("/orders/{orderId}")
	public Result<OrderVO> detail(@PathVariable("orderId") Long orderId) {
		return Result.success(orderQueryService.getDetail(orderId));
	}

	/**
	 * M2-04 取消订单
	 *
	 * @param orderId 订单ID
	 * @param reason  取消原因，可选
	 * @return 空结果
	 */
	@PostMapping("/orders/{orderId}/cancel")
	public Result<Void> cancel(@PathVariable("orderId") Long orderId,
			@RequestParam(value = "reason", required = false) String reason) {
		orderService.cancel(orderId, reason);
		return Result.ok();
	}

	/**
	 * M2-05 作废违规订单（经理）
	 *
	 * @param orderId 订单ID
	 * @param reason  作废原因，必填
	 * @return 审计ID
	 */
	@DeleteMapping("/orders/{orderId}")
	public Result<String> invalidate(@PathVariable("orderId") Long orderId,
			@RequestParam("reason") String reason) {
		return Result.success(orderService.invalidate(orderId, reason));
	}

	/**
	 * M2-07 分页查询订单
	 *
	 * @param query 查询条件
	 * @return 分页订单
	 */
	@GetMapping("/orders")
	public Result<PageResult<OrderVO>> page(@ModelAttribute OrderQuery query) {
		return Result.success(orderService.page(query));
	}
}
