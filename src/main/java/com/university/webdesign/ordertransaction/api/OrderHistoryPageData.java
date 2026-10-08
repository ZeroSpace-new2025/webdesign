package com.university.webdesign.ordertransaction.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * “我的订单”页视图数据
 * <p>
 * 承载个人历史订单查询结果与今日订单的可操作状态。
 * 页面只展示 {@link #rows}，其中每行预先算好了“能否支付/能否取消”，
 * 避免在模板里硬编码订单状态文案。
 */
@Data
public class OrderHistoryPageData
{
	/**
	 * 当前操作员工ID，尚未设置身份时为 null
	 */
	private Long userId;
	
	/**
	 * 就餐日期（业务时区下的“今天”）
	 */
	private LocalDate today;
	
	/**
	 * 订餐截止时间文本，例如 09:00，用于页面提示
	 */
	private String cutoffTime;
	
	/**
	 * 当前是否仍在可修改窗口内（截止前），决定取消按钮是否可用
	 */
	private boolean canModify;
	
	/**
	 * 是否查询全部历史（未指定日期范围时默认只查本月）
	 */
	private boolean showAll;
	
	/**
	 * 查询起始日期（含），为 null 表示不限
	 */
	private LocalDate start;
	
	/**
	 * 查询结束日期（含），为 null 表示不限
	 */
	private LocalDate end;
	
	/**
	 * 历史订单行（按下单时间升序），已取消订单也会出现在个人历史中
	 */
	private List<OrderRow> rows = new ArrayList<>();
	
	/**
	 * 查询到的订单张数
	 */
	private int orderCount;
	
	/**
	 * 消费合计（不含已取消订单）
	 */
	private BigDecimal totalAmount = BigDecimal.ZERO;
	
	/**
	 * 今日的有效订单行，没有时为 null（与 {@link #rows} 中的行同构，便于统一渲染）
	 */
	private OrderRow todayRow;
	
	/**
	 * 历史订单行
	 * <p>
	 * {@code payable} / {@code cancellable} / {@code cancelled} 由服务端根据订单状态、时间窗口算好，
	 * 模板只负责按布尔值显示按钮与样式，不在页面里硬编码订单状态文案。
	 */
	@Data
	public static class OrderRow
	{
		/**
		 * 订单（含明细快照）
		 */
		private OrderDTO order;
		
		/**
		 * 下单时间文本（yyyy-MM-dd HH:mm），已按业务时区格式化
		 */
		private String createdAtText;
		
		/**
		 * 是否可支付（未支付订单可支付）
		 */
		private boolean payable;
		
		/**
		 * 是否可取消（未支付/已支付且未过截止时间，且订单属于今天）
		 */
		private boolean cancellable;
		
		/**
		 * 是否已取消（用于列表上的置灰样式）
		 */
		private boolean cancelled;
	}
}
