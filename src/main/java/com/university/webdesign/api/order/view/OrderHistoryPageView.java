package com.university.webdesign.api.order.view;

import com.university.webdesign.service.order.dto.OrderVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 我的订单页视图数据。
 * <p>
 * 展示今日订单（可取消）与历史订单列表；页面上的“可取消”判定同样来自 service 层的时间窗口，
 * 不在模板里自己算时间。
 */
@Data
public class OrderHistoryPageView
{
	/**
	 * 当前登录员工ID
	 */
	private Long userId;

	/**
	 * 当前登录员工姓名
	 */
	private String userName;

	/**
	 * 查询起始日期（含）
	 */
	private LocalDate start;

	/**
	 * 查询结束日期（含）
	 */
	private LocalDate end;

	/**
	 * 是否查询全部历史
	 */
	private boolean showAll;

	/**
	 * 订餐截止时间文本
	 */
	private String cutoffTime;

	/**
	 * 当前是否仍可取消今日订单
	 */
	private boolean canModify;

	/**
	 * 今日订单，没有时为 null
	 */
	private OrderVO todayOrder;

	/**
	 * 历史订单行
	 */
	private List<OrderRow> rows = new ArrayList<>();

	/**
	 * 订单总数
	 */
	private int orderCount;

	/**
	 * 消费合计（不含已取消/已作废）
	 */
	private BigDecimal totalAmount = BigDecimal.ZERO;

	/**
	 * 历史订单行（订单 + 展示文案）
	 */
	@Data
	public static class OrderRow
	{
		/**
		 * 订单视图
		 */
		private OrderVO order;

		/**
		 * 下单时间文案
		 */
		private String createdAtText;

		/**
		 * 是否已取消/已作废
		 */
		private boolean cancelled;

		/**
		 * 是否可取消（本人 + 截止前 + 待确认）
		 */
		private boolean cancellable;
	}
}
