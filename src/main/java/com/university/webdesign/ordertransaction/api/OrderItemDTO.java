package com.university.webdesign.ordertransaction.api;

import com.university.webdesign.ordertransaction.data.Order;
import com.university.webdesign.ordertransaction.data.OrderItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单明细DTO
 * <p>
 * 对外暴露的是下单时刻的菜名/单价快照，而不是菜谱模块的当前数据。
 */
public class OrderItemDTO
{
	/**
	 * 菜品（菜谱）ID
	 */
	private Long itemId;
	
	/**
	 * 下单时的菜名
	 */
	private String itemName;
	
	/**
	 * 下单时的菜品分类
	 */
	private String category;
	
	/**
	 * 下单时的单价
	 */
	private BigDecimal unitPrice;
	
	/**
	 * 数量
	 */
	private Integer quantity;
	
	/**
	 * 小计金额
	 */
	private BigDecimal subtotal;
	
	public OrderItemDTO() {
	}
	
	public OrderItemDTO(Long itemId, String itemName, String category, BigDecimal unitPrice, Integer quantity) {
		this.itemId = itemId;
		this.itemName = itemName;
		this.category = category;
		this.unitPrice = unitPrice;
		this.quantity = quantity;
		this.subtotal = unitPrice == null || quantity == null
				? BigDecimal.ZERO
				: unitPrice.multiply(BigDecimal.valueOf(quantity));
	}
	
	/**
	 * 由实体转换为 DTO
	 *
	 * @param item 订单明细实体
	 * @return 明细DTO
	 */
	public static OrderItemDTO from(OrderItem item) {
		return new OrderItemDTO(item.getItemId(), item.getItemName(), item.getCategory(),
				item.getUnitPrice(), item.getQuantity());
	}
	
	/**
	 * 由实体列表批量转换为 DTO 列表
	 *
	 * @param items 订单明细实体列表
	 * @return 明细DTO列表，入参为空时返回空列表
	 */
	public static List<OrderItemDTO> fromAll(List<OrderItem> items) {
		List<OrderItemDTO> result = new ArrayList<>();
		if (items == null) {
			return result;
		}
		for (OrderItem item : items) {
			result.add(from(item));
		}
		return result;
	}
	
	/**
	 * 由订单实体提取明细DTO
	 *
	 * @param order 订单实体
	 * @return 明细DTO列表
	 */
	public static List<OrderItemDTO> fromOrder(Order order) {
		return fromAll(order.getItems());
	}
	
	public Long getItemId() {
		return itemId;
	}
	
	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}
	
	public String getItemName() {
		return itemName;
	}
	
	public void setItemName(String itemName) {
		this.itemName = itemName;
	}
	
	public String getCategory() {
		return category;
	}
	
	public void setCategory(String category) {
		this.category = category;
	}
	
	public BigDecimal getUnitPrice() {
		return unitPrice;
	}
	
	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
	}
	
	public Integer getQuantity() {
		return quantity;
	}
	
	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
	
	public BigDecimal getSubtotal() {
		return subtotal;
	}
	
	public void setSubtotal(BigDecimal subtotal) {
		this.subtotal = subtotal;
	}
}
