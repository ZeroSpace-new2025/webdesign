package com.university.webdesign.domain.order;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;

/**
 * 订单状态。
 * <p>
 * 对应《对外方法表》M2 与需求原文：
 * <ul>
 *     <li>{@link #PENDING}：员工提交后的待确认状态，占用当日名额；</li>
 *     <li>{@link #VALID}：已确认的有效订单，参与聚合、配送与报表；</li>
 *     <li>{@link #INVALID}：经理作废的违规订单（逻辑删除，留痕，不物理删除）；</li>
 *     <li>{@link #CANCELLED}：员工本人在截止前取消的订单。</li>
 * </ul>
 * 有效订单 = {@link #PENDING} 或 {@link #VALID}，只有有效订单占用“一人一天一单”名额，
 * 也只有有效订单参与总括订单聚合与配送单生成。
 */
public enum OrderStatus
{
	/**
	 * 待确认（有效）
	 */
	PENDING("待确认"),

	/**
	 * 已确认（有效）
	 */
	VALID("已确认"),

	/**
	 * 已作废（经理删单留痕）
	 */
	INVALID("已作废"),

	/**
	 * 已取消（员工本人取消）
	 */
	CANCELLED("已取消");

	/**
	 * 中文文案
	 */
	private final String text;

	OrderStatus(String text) {
		this.text = text;
	}

	/**
	 * @return 中文文案
	 */
	public String getText() {
		return text;
	}

	/**
	 * 是否为有效订单（占用当日名额、参与聚合与配送）
	 *
	 * @return 待确认或已确认时返回 true
	 */
	public boolean isActive() {
		return this == PENDING || this == VALID;
	}

	/**
	 * 是否为已完结状态（不可再流转）
	 *
	 * @return 已作废或已取消时返回 true
	 */
	public boolean isFinished() {
		return this == INVALID || this == CANCELLED;
	}

	/**
	 * 是否允许员工本人改单
	 * <p>
	 * 只有待确认的订单可以改；已确认后厨房可能已开始备料，员工不能再自行改动。
	 *
	 * @return 待确认时返回 true
	 */
	public boolean isEditable() {
		return this == PENDING;
	}

	/**
	 * 解析状态编码，忽略大小写
	 *
	 * @param status 状态编码，例如 {@code PENDING}
	 * @return 订单状态
	 */
	public static OrderStatus parse(String status) {
		if (status == null || status.isBlank()) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "订单状态不能为空");
		}
		for (OrderStatus value : values()) {
			if (value.name().equalsIgnoreCase(status.trim())) {
				return value;
			}
		}
		// 兼容旧前端传入的中文状态名
		for (OrderStatus value : values()) {
			if (value.text.equals(status.trim())) {
				return value;
			}
		}
		throw new BusinessException(ErrorCode.PARAM_INVALID, "未知的订单状态：" + status);
	}
}
