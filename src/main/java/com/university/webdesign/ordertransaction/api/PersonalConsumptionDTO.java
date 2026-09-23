package com.university.webdesign.ordertransaction.api;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 个人月度消费统计DTO
 * <p>
 * 对应用户可查看并打印的“个人月度消费统计”，以及财务/经理查询员工消费明细时使用的数据结构。
 */
@Data
public class PersonalConsumptionDTO
{
	/**
	 * 员工ID
	 */
	private Long userId;
	
	/**
	 * 统计月份，格式 yyyy-MM
	 */
	private String month;
	
	/**
	 * 该月有效订单数
	 */
	private int orderCount;
	
	/**
	 * 该月消费总金额
	 */
	private BigDecimal totalAmount = BigDecimal.ZERO;
	
	/**
	 * 该月消费明细（逐单明细，按下单时间升序）
	 */
	private List<OrderDTO> orders = new ArrayList<>();
	
	/**
	 * 该月按菜品汇总的消费情况
	 */
	private List<ItemConsumptionDTO> itemSummaries = new ArrayList<>();
	
	/**
	 * 单菜品消费汇总
	 */
	@Data
	public static class ItemConsumptionDTO
	{
		/**
		 * 菜品（菜谱）ID
		 */
		private Long itemId;
		
		/**
		 * 菜名（下单时的快照）
		 */
		private String itemName;
		
		/**
		 * 累计数量
		 */
		private long quantity;
		
		/**
		 * 累计金额
		 */
		private BigDecimal amount = BigDecimal.ZERO;
	}
}
