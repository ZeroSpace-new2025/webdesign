package com.university.webdesign.ordertransaction.api;

import org.jspecify.annotations.NonNull;

/**
 * 订单编辑 API 接口
 * @remark 所有涉及删除的操作都是逻辑删除，数据库中仍然保留数据
 */
@SuppressWarnings("UnusedDeclaration")
public interface OrderEditApi
{
	/**
	 * 更新订单状态
	 *
	 * @param orderId 订单 ID
	 * @param status 订单状态
	 * @remark 唯一直接更改数据库中订单数据的方法
	 */
	void updateOrderStatus(long orderId,@NonNull OrderState status);
}
