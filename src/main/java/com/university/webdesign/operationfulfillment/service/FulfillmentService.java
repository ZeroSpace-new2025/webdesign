package com.university.webdesign.operationfulfillment.service;

import com.university.webdesign.operationfulfillment.api.BlanketOrderDTO;
import com.university.webdesign.operationfulfillment.api.DeliveryTaskDTO;
import com.university.webdesign.operationfulfillment.api.DeliveryTaskQueryData;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 运营与履约服务。
 * <p>
 * 面向后厨和配送环节，处理“订餐截止时间”后的数据聚合与配送管理：
 * <ul>
 *     <li>总括订单：聚合当日所有有效订单，按菜品分类统计总数量，供厨房备料与生产单打印。</li>
 *     <li>配送管理：到达“配餐开始时间”（默认 11:30）后开放打印权限，按员工/工位维度批量生成配送单。</li>
 * </ul>
 */
@Component
public interface FulfillmentService
{
	/* ==================== 总括订单 (Blanket Order) ==================== */

	/**
	 * 触发当日总括订单的聚合生成。
	 * <p>
	 * 仅在“订餐截止时间”之后允许执行：汇总当日所有有效订单 → 按菜品分类统计总数量 → 写入 Daily_Statistics 快照。
	 *
	 * @return 当日总括订单
	 */
	BlanketOrderDTO generateBlanketOrder();

	/**
	 * 按日期获取总括订单（生产单）。
	 *
	 * @param date 日期（毫秒时间戳）
	 * @return 总括订单；若不存在返回 null
	 */
	BlanketOrderDTO getBlanketOrder(Long date);

	/**
	 * 获取今日总括订单。
	 *
	 * @return 今日总括订单；若尚未聚合返回 null
	 */
	BlanketOrderDTO getTodayBlanketOrder();

	/* ==================== 配送管理 (Delivery) ==================== */

	/**
	 * 判断当前是否已开放配送单打印权限（到达“配餐开始时间”后开放）。
	 *
	 * @return true 表示已过配餐开始时间，允许打印
	 */
	boolean canPrintDelivery();

	/**
	 * 批量生成当日配送任务（按员工/工位维度拆分，包含菜名、分量、工位、电话）。
	 * <p>
	 * 调用前应先通过 {@link #canPrintDelivery()} 校验时间窗口，未到配餐开始时间应拒绝生成。
	 *
	 * @return 当日生成的配送任务列表
	 */
	List<DeliveryTaskDTO> generateDeliveryTasks();

	/**
	 * 按条件查询配送任务。
	 *
	 * @param queryData 查询条件
	 * @return 配送任务列表
	 */
	List<DeliveryTaskDTO> queryDeliveryTasks(DeliveryTaskQueryData queryData);

	/**
	 * 获取指定配送任务详情。
	 *
	 * @param taskId 配送任务 ID
	 * @return 配送任务；若不存在返回 null
	 */
	DeliveryTaskDTO getDeliveryTask(Long taskId);
}
