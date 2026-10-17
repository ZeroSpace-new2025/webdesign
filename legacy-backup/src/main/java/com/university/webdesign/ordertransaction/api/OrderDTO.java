package com.university.webdesign.ordertransaction.api;

import com.university.webdesign.ordertransaction.data.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单DTO
 */
@Data
public class OrderDTO
{
	/**
	 * 订单ID
	 */
	private Long orderId;
	
	/**
	 * 订单号（对外展示、打印用）
	 */
	private Long orderNumber;
	
	/**
	 * 用户ID
	 */
	private Long userId;
	
	/**
	 * 订单创建时间（毫秒时间戳）
	 */
	private long createdTime;
	
	/**
	 * 订单最后修改时间（毫秒时间戳），未修改过时为 0
	 */
	private long lastModifiedTime;
	
	/**
	 * 订单状态（中文，见 {@code OrderStatus}）
	 */
	private String status;
	
	/**
	 * 订单总金额
	 */
	private BigDecimal total;
	
	/**
	 * 订单项ID列表（菜品/菜谱 ID，便于前端回填勾选状态）
	 */
	private List<Long> itemIds;
	
	/**
	 * 订单明细（含下单时的菜名、单价、数量快照）
	 */
	private List<OrderItemDTO> items;
	
	/**
	 * 由实体转换为 DTO
	 * <p>
	 * 调用方需保证订单明细已加载（未开启延迟加载的游离环境请在事务内调用）。
	 *
	 * @param order 订单实体
	 * @return 订单DTO
	 */
	public static OrderDTO from(Order order) {
		OrderDTO dto = new OrderDTO();
		dto.setOrderId(order.getId());
		dto.setOrderNumber(order.getOrderNumber());
		dto.setUserId(order.getUserId());
		dto.setCreatedTime(toEpochMilli(order.getOrderDate()));
		dto.setLastModifiedTime(toEpochMilli(order.getLastModifiedTime()));
		dto.setStatus(order.getStatus() == null ? null : order.getStatus().toString());
		dto.setTotal(order.getTotal());
		dto.setItems(OrderItemDTO.fromOrder(order));
		
		List<Long> itemIds = new ArrayList<>();
		for (OrderItemDTO item : dto.getItems()) {
			if (item.getItemId() != null) {
				itemIds.add(item.getItemId());
			}
		}
		dto.setItemIds(itemIds);
		return dto;
	}
	
	/**
	 * 由实体列表批量转换为 DTO 列表
	 *
	 * @param orders 订单实体列表
	 * @return 订单DTO列表，入参为空时返回空列表
	 */
	public static List<OrderDTO> fromAll(List<Order> orders) {
		List<OrderDTO> result = new ArrayList<>();
		if (orders == null) {
			return result;
		}
		for (Order order : orders) {
			result.add(from(order));
		}
		return result;
	}
	
	/**
	 * 时间转换：LocalDateTime → 毫秒时间戳
	 *
	 * @param time 时间，可为 null
	 * @return 毫秒时间戳；入参为 null 时返回 0
	 */
	public static long toEpochMilli(LocalDateTime time) {
		if (time == null) {
			return 0L;
		}
		return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
}
