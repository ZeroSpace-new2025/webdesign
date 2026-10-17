package com.university.webdesign.ordertransaction.repository;

import com.university.webdesign.ordertransaction.data.Order;
import com.university.webdesign.ordertransaction.data.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单明细仓储
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long>
{
	/**
	 * 查询某张订单的明细
	 *
	 * @param order 订单实体
	 * @return 明细列表
	 */
	List<OrderItem> findByOrder(Order order);
}
