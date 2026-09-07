package com.university.webdesign.ordertransaction.api;

/**
 * 订单状态枚举类
 */
@SuppressWarnings("UnusedDeclaration")
public enum OrderState
{
	/**
	 * 订单状态：已上传
	 */
	UPLOADED,
	/**
	 * 订单状态：已完成
	 */
	COMPLETED,
	/**
	 * 订单状态：已取消
	 */
	CANCELLED,
	/**
	 * 订单状态：未支付
	 */
	NOT_PAID,
	/**
	 * 订单状态：未保存
	 */
	NOT_SAVED,
	/**
	 * 订单状态：正在制作
	 */
	MAKING,
	/**
	 * 订单状态：正在配送
	 */
	DELIVERING,
}
