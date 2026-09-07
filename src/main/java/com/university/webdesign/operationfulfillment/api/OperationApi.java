package com.university.webdesign.operationfulfillment.api;

@SuppressWarnings("UnusedDeclaration")
public interface OperationApi
{
	/**
	 * 拉取订单数据
	 * @remark 该方法会从数据库中拉取所有订单数据，并将其存储在内存中，以便后续操作使用
	 */
	void pullOrderData();
	
	/**
	 * 同步订单数据
	 * @remark 该方法会将内存中的订单数据同步到数据库中，确保数据库中的数据与内存中的数据一致
	 *
	 * @param orderId 订单 ID
	 */
	void syncOrderData(long orderId);
}
