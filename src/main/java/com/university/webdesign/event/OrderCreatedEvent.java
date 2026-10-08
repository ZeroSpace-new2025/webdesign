package com.university.webdesign.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 订单创建事件（M2 发布，M4 报表订阅）。
 * <p>
 * 对应《对外方法表》3.3：载荷 `orderId`、`employeeId`、`items[]`、`totalAmount`。
 *
 * @param orderId     订单ID
 * @param employeeId  员工ID
 * @param orderDate   就餐日期
 * @param items       明细快照
 * @param totalAmount 订单总金额
 */
public record OrderCreatedEvent(
		Long orderId,
		Long employeeId,
		LocalDate orderDate,
		List<Item> items,
		BigDecimal totalAmount)
{
	/**
	 * 事件载荷中的明细快照。
	 *
	 * @param recipeId   菜品ID
	 * @param recipeName 菜名快照
	 * @param category   分类快照
	 * @param quantity   数量
	 * @param amount     小计
	 */
	public record Item(Long recipeId, String recipeName, String category, Integer quantity, BigDecimal amount)
	{
	}
}
