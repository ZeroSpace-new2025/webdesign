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
		};
	}
	
	/**
	 * 由中文状态名解析枚举
	 *
	 * @param status 中文状态名，例如“未支付”
	 * @return 对应枚举；无法识别时返回 {@code null}
	 */
	public static OrderStatus fromString(String status) {
		if (status == null) {
			return null;
		}
		for (OrderStatus value : values()) {
			if (value.toString().equals(status)) {
				return value;
			}
		}
		return null;
	}
	
	/**
	 * 是否为“有效订单”（占用当日点餐名额、参与后厨与配送流程）
	 *
	 * @return 非取消状态的订单返回 {@code true}
	 */
	public boolean isActive() {
		return this != CANCELLED;
	}
	
	/**
	 * 是否为“未进入履约环节”的订单
	 * <p>
	 * 只有处于这些状态的订单才允许员工自行修改或取消。
	 *
	 * @return 未支付或已支付时返回 {@code true}
	 */
	public boolean isEditable() {
		return this == UNPAID || this == PAID;
	}
}
