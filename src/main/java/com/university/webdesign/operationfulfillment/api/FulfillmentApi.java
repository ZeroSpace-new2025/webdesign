package com.university.webdesign.operationfulfillment.api;

import com.university.webdesign.common.Result;
import com.university.webdesign.operationfulfillment.service.FulfillmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.List;

/**
 * 运营与履约 API。
 * <p>
 * 面向后厨和配送环节，对外提供两类能力：
 * <ul>
 *     <li>总括订单（生产单）：在“订餐截止时间”后聚合当日有效订单，按菜品分类统计总数量，供厨房备料。</li>
 *     <li>配送管理：到达“配餐开始时间”（默认 11:30）后开放打印权限，按员工/工位批量生成配送单。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/fulfillment")
public class FulfillmentApi
{
	private final FulfillmentService fulfillmentService;

	public FulfillmentApi(FulfillmentService fulfillmentService) {
		this.fulfillmentService = fulfillmentService;
	}

	/* ==================== 总括订单 (Blanket Order) ==================== */

	/**
	 * 触发当日总括订单（生产单）的聚合生成。
	 * <p>
	 * 在“订餐截止时间”后调用，汇总当日所有有效订单并按菜品分类统计总数量。
	 *
	 * @return 当日总括订单
	 */
	@PostMapping("/blanket-order/generate")
	public Result<BlanketOrderDTO> generateBlanketOrder() {
		var blanketOrder = fulfillmentService.generateBlanketOrder();
		return Result.success(blanketOrder);
	}

	/**
	 * 获取今日总括订单（生产单）。
	 *
	 * @return 今日总括订单；若尚未聚合返回 null
	 */
	@GetMapping("/blanket-order/today")
	public Result<BlanketOrderDTO> getTodayBlanketOrder() {
		var blanketOrder = fulfillmentService.getTodayBlanketOrder();
		return Result.success(blanketOrder);
	}

	/**
	 * 按日期获取总括订单（生产单）。
	 *
	 * @param date 日期（毫秒时间戳）
	 * @return 总括订单；若不存在返回 null
	 */
	@GetMapping("/blanket-order/{date}")
	public Result<BlanketOrderDTO> getBlanketOrder(@PathVariable("date") Long date) {
		var blanketOrder = fulfillmentService.getBlanketOrder(date);
		return Result.success(blanketOrder);
	}

	/* ==================== 配送管理 (Delivery) ==================== */

	/**
	 * 检查当前是否已开放配送单打印权限（到达“配餐开始时间”后开放）。
	 *
	 * @return true 表示允许打印配送单
	 */
	@GetMapping("/delivery/printable")
	public Result<Boolean> canPrintDelivery() {
		return Result.success(fulfillmentService.canPrintDelivery());
	}

	/**
	 * 批量生成当日配送任务（按员工/工位维度，包含菜名、分量、工位、电话）。
	 * <p>
	 * 仅在到达“配餐开始时间”后允许执行；服务层会校验时间窗口。
	 *
	 * @return 当日生成的配送任务列表
	 */
	@PostMapping("/delivery-tasks/generate")
	public Result<List<DeliveryTaskDTO>> generateDeliveryTasks() {
		var tasks = fulfillmentService.generateDeliveryTasks();
		return Result.success(tasks);
	}

	/**
	 * 按条件查询配送任务。
	 *
	 * @param queryData 查询条件
	 * @return 配送任务列表
	 */
	@PostMapping("/delivery-tasks/query")
	public Result<List<DeliveryTaskDTO>> queryDeliveryTasks(@RequestBody DeliveryTaskQueryData queryData) {
		var tasks = fulfillmentService.queryDeliveryTasks(queryData);
		return Result.success(tasks);
	}

	/**
	 * 获取指定配送任务详情。
	 *
	 * @param id 配送任务 ID
	 * @return 配送任务；若不存在返回 null
	 */
	@GetMapping("/delivery-tasks/{id}")
	public Result<DeliveryTaskDTO> getDeliveryTask(@PathVariable("id") Long id) {
		var task = fulfillmentService.getDeliveryTask(id);
		return Result.success(task);
	}
}
