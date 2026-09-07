package com.university.webdesign.ordertransaction.data;

import com.university.webdesign.ordertransaction.api.OrderState;
import lombok.Data;

import java.util.List;

@Data
public class Order
{
	private OrderState orderState;
	private long orderId;
	private long userId;
	private long time;
	private List<Long> recipeIds;
}
