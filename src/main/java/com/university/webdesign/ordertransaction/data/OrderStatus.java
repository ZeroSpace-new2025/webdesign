package com.university.webdesign.ordertransaction.data;

/**
 * 订单状态枚举类
 */

public enum OrderStatus
{
	/**
	 * 未支付
	 */
	UNPAID,
	/**
	 * 已支付
	 */
	PAID,
	/**
	 * 已送达
	 */
	DELIVERED,
	/**
	 * 已完成
	 */
	COMPLETED,
	/**
	 * 已取消
	 */
	CANCELLED,
	/**
	 * 处理中
	 */
	PROCESSING,
	/**
	 * 已出餐
	 */
	READY;
	
	@Override
	
	public String toString() {
		return switch (this) {
			case UNPAID -> "未支付";
			case PAID -> "已支付";
			case DELIVERED -> "已送达";
			case COMPLETED -> "已完成";
			case CANCELLED -> "已取消";
			case PROCESSING -> "处理中";
			case READY -> "已出餐";
			default -> "";
		};
	}
	
	public static OrderStatus fromString(String status) {
		return switch (status) {
			case "未支付" -> UNPAID;
			case "已支付" -> PAID;
			case "已送达" -> DELIVERED;
			case "已完成" -> COMPLETED;
			case "已取消" -> CANCELLED;
			case "处理中" -> PROCESSING;
			case "已出餐" -> READY;
			default -> null;
		};
	}
	}