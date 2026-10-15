package com.university.webdesign.api.operation;

import lombok.Data;

/**
 * 更新配送状态入参（M3-11）。
 * <p>
 * 对应《对外方法表》M3-11 的 `status`、`receiver`、`remark`；
 * 状态机校验（禁止跳跃/回退）在 service 层完成，非法流转返回 40902。
 */
@Data
public class DeliveryStatusRequest
{
	/**
	 * 目标状态：PENDING / DELIVERING / DELIVERED / EXCEPTION
	 */
	private String status;

	/**
	 * 签收人
	 */
	private String receiver;

	/**
	 * 备注
	 */
	private String remark;
}
