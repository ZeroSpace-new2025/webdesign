package com.university.webdesign.ordertransaction.api;

import lombok.Data;

import java.util.List;

/**
 * 改单请求体
 * <p>
 * //todo 确认：字段校验目前在 {@code OrderServiceImpl} 服务层完成，理由同 {@link OrderCreateData}。
 */
@Data
public class OrderUpdateData
{
	/**
	 * 要修改的订单ID
	 */
	private Long orderId;
	
	/**
	 * 发起修改的操作用户ID
	 * <p>
	 * //todo 确认：同 {@link OrderCreateData#getOperatorId()}，接入登录态后应改为从认证上下文中取。
	 */
	private Long operatorId;
	
	/**
	 * 修改后的菜品（菜谱）ID列表
	 */
	private List<Long> itemIds;
	
	/**
	 * 与 {@link #itemIds} 一一对应的数量列表
	 */
	private List<Integer> quantities;
}
