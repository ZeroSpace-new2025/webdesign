package com.university.webdesign.web.order;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.config.RequiresPerm;
import com.university.webdesign.domain.order.OrderStatus;
import com.university.webdesign.service.order.OrderQueryService;
import com.university.webdesign.service.order.OrderService;
import com.university.webdesign.service.order.dto.OrderQuery;
import com.university.webdesign.service.order.dto.OrderVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * 订单管理页面控制器（服务端渲染，走 {@code /order/manage/**}，餐厅经理使用）。
 * <p>
 * 对应需求原文第 3 条：“餐厅经理可以删除员工已经提交的订单”，以及需求文档 M2 的
 * “订单管理页面：餐厅经理查看所有订单，支持删除违规订单”。
 * <p>
 * 两个约束必须守住：
 * <ul>
 *     <li><b>删除是作废留痕</b>：走 {@code OrderService.invalidate}（{@code status=INVALID} +
 *         作废人/原因/时间），不做物理删除，历史统计可追溯；</li>
 *     <li><b>取数只走 service</b>：列表用 {@link OrderQueryService#page}，并且
 *         {@code includeInactive=true} 才能看到已取消/已作废的单，页面不直接查订单表。</li>
 * </ul>
 * 访问控制：需要 {@link PermissionEnum#ORDER_INVALIDATE} 权限点（只有餐厅经理具备）。
 */
@Controller
@RequestMapping("/order/manage")
@RequiresPerm(PermissionEnum.ORDER_INVALIDATE)
public class OrderManagePageController
{
	/**
	 * 单页最多展示的订单数（课程项目数据量小，一屏看完再筛选即可）
	 */
	private static final int PAGE_SIZE = 200;

	private final OrderQueryService orderQueryService;
	private final OrderService orderService;

	/**
	 * 构造器注入
	 *
	 * @param orderQueryService 订单查询契约（跨模块只读入口，本模块内直接用）
	 * @param orderService      订单写服务（作废）
	 */
	public OrderManagePageController(OrderQueryService orderQueryService, OrderService orderService) {
		this.orderQueryService = orderQueryService;
		this.orderService = orderService;
	}

	/**
	 * 订单管理列表：按就餐日期区间 + 状态筛选，默认查今天
	 *
	 * @param dateFrom 起始日期（含），可选
	 * @param dateTo   结束日期（含），可选
	 * @param status   订单状态编码，可选（空表示全部状态）
	 * @param model    视图模型
	 * @return 视图名
	 */
	@GetMapping
	public String page(
			@RequestParam(value = "dateFrom", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
			@RequestParam(value = "dateTo", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
			@RequestParam(value = "status", required = false) String status,
			Model model) {
		LocalDate from = dateFrom == null ? LocalDate.now() : dateFrom;
		LocalDate to = dateTo == null ? from : dateTo;
		if (to.isBefore(from)) {
			LocalDate swap = from;
			from = to;
			to = swap;
		}
		OrderQuery query = new OrderQuery();
		query.setDateFrom(from);
		query.setDateTo(to);
		query.setStatus(status == null || status.isBlank() ? null : status.trim());
		// 经理视角要能看到取消/作废的单，否则“作废后去哪儿了”无从核对
		query.setIncludeInactive(Boolean.TRUE);
		query.setPageNum(1);
		query.setPageSize(PAGE_SIZE);
		PageResult<OrderVO> result = orderQueryService.page(query);

		model.addAttribute("orders", result.getList());
		model.addAttribute("total", result.getTotal());
		model.addAttribute("dateFrom", from);
		model.addAttribute("dateTo", to);
		model.addAttribute("status", status);
		model.addAttribute("allStatus", OrderStatus.values());
		// 快捷筛选用的“今天 / 本月”，日期由服务端给出，页面不自己算
		model.addAttribute("today", LocalDate.now());
		model.addAttribute("monthStart", LocalDate.now().withDayOfMonth(1));
		return "ordertransaction/order-manage";
	}

	/**
	 * 作废违规订单（逻辑删除，留痕）
	 *
	 * @param id       订单ID
	 * @param reason   作废原因（必填，写入审计字段）
	 * @param dateFrom 列表筛选：起始日期
	 * @param dateTo   列表筛选：结束日期
	 * @param status   列表筛选：状态
	 * @param redirect 重定向属性
	 * @return 返回列表（保留筛选条件）
	 */
	@PostMapping("/{id}/invalidate")
	public String invalidate(
			@PathVariable("id") Long id,
			@RequestParam("reason") String reason,
			@RequestParam(value = "dateFrom", required = false) String dateFrom,
			@RequestParam(value = "dateTo", required = false) String dateTo,
			@RequestParam(value = "status", required = false) String status,
			RedirectAttributes redirect) {
		try {
			orderService.invalidate(id, reason);
			redirect.addFlashAttribute("message", "订单已作废（逻辑删除，已记录作废原因与操作人）");
		} catch (RuntimeException exception) {
			redirect.addFlashAttribute("error", exception.getMessage());
		}
		redirect.addAttribute("dateFrom", dateFrom);
		redirect.addAttribute("dateTo", dateTo);
		if (status != null && !status.isBlank()) {
			redirect.addAttribute("status", status);
		}
		return "redirect:/order/manage";
	}
}
