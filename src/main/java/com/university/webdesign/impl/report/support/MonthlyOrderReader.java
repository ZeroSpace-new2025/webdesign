package com.university.webdesign.impl.report.support;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.service.order.OrderQueryService;
import com.university.webdesign.service.order.dto.OrderQuery;
import com.university.webdesign.service.order.dto.OrderVO;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 月度订单读取器（M4 报表与消费审计共用的取数适配层）。
 * <p>
 * 硬性边界：**只经 {@link OrderQueryService} 读取订单**，不新建订单实体、不直接访问
 * {@code order_form} / {@code order_detail} 表。报表服务与消费审计服务都调用本类，
 * 保证“跨模块取数只有一条路径”，也便于单测替换。
 */
public class MonthlyOrderReader
{
	/**
	 * 逐页拉取订单时每页条数（{@code PageQuery} 上限）
	 */
	private static final int PAGE_SIZE = 100;

	/**
	 * 逐页拉取订单的最大页数，防止异常数据导致死循环
	 */
	private static final int MAX_PAGES = 400;

	private final OrderQueryService orderQueryService;

	/**
	 * 构造器注入
	 *
	 * @param orderQueryService 订单取数契约（跨模块，只读）
	 */
	public MonthlyOrderReader(OrderQueryService orderQueryService) {
		this.orderQueryService = orderQueryService;
	}

	/**
	 * 读取某月全部有效订单（含下单时刻明细快照）
	 * <p>
	 * 已过完的月份取整月；当前月只取到今天为止，避免把“未来的订单日期”算进来。
	 *
	 * @param month 月份
	 * @return 有效订单列表
	 */
	public List<OrderVO> readMonth(YearMonth month) {
		return readMonth(month, null);
	}

	/**
	 * 读取某月有效订单，可按员工过滤
	 *
	 * @param month       月份
	 * @param employeeIds 限定员工ID集合，{@code null} 表示不限
	 * @return 有效订单列表
	 */
	public List<OrderVO> readMonth(YearMonth month, Set<Long> employeeIds) {
		if (month == null) {
			return List.of();
		}
		LocalDate end = month.atEndOfMonth();
		LocalDate today = LocalDate.now();
		if (month.equals(YearMonth.from(today))) {
			end = today;
		}
		List<OrderVO> orders = new ArrayList<>();
		for (LocalDate date = month.atDay(1); !date.isAfter(end); date = date.plusDays(1)) {
			Set<Long> known = new LinkedHashSet<>();
			for (int pageNum = 1; pageNum <= MAX_PAGES; pageNum++) {
				OrderQuery query = new OrderQuery();
				query.setDateFrom(date);
				query.setDateTo(date);
				query.setPageNum(pageNum);
				query.setPageSize(PAGE_SIZE);
				PageResult<OrderVO> page = orderQueryService.page(query);
				if (page == null || page.getList() == null || page.getList().isEmpty()) {
					break;
				}
				boolean added = false;
				for (OrderVO order : page.getList()) {
					if (order == null || order.getOrderId() == null || !known.add(order.getOrderId())) {
						continue;
					}
					if (employeeIds != null && !employeeIds.contains(order.getEmployeeId())) {
						continue;
					}
					orders.add(fulfilItems(order));
					added = true;
				}
				if (page.getList().size() < PAGE_SIZE || !added) {
					break;
				}
			}
		}
		return orders;
	}

	/**
	 * 补全明细：列表视图若未带明细，再按ID取一次详情
	 *
	 * @param order 订单视图
	 * @return 含明细的订单视图
	 */
	private OrderVO fulfilItems(OrderVO order) {
		if (order.getItems() != null && !order.getItems().isEmpty()) {
			return order;
		}
		OrderVO detail = orderQueryService.getDetail(order.getOrderId());
		return detail == null ? order : detail;
	}
}
