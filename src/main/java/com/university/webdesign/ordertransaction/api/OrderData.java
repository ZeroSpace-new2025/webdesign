package com.university.webdesign.ordertransaction.api;

import com.university.webdesign.ordertransaction.data.Order;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@SuppressWarnings("UnusedDeclaration")
public class OrderData
{
	private OrderState orderState;
	private long orderId;
	private long userId;
	private long time;
	@NonNull
	private List<Long> recipeIds;
	
	public boolean isHistoryOrder() {
		return orderState == OrderState.COMPLETED || orderState == OrderState.CANCELLED;
	}
	
	public OrderData(@NonNull Order order) {
		this.orderState = order.getOrderState();
		this.orderId = order.getOrderId();
		this.userId = order.getUserId();
		this.time = order.getTime();
		this.recipeIds = order.getRecipeIds() == null ? List.of() : List.copyOf(order.getRecipeIds());
	}
}
