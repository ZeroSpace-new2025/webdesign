package com.university.webdesign.service.impl.order;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.TabularExport;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.domain.order.OrderDetail;
import com.university.webdesign.domain.order.OrderForm;
import com.university.webdesign.domain.order.OrderStatus;
import com.university.webdesign.repository.order.OrderFormRepository;
import com.university.webdesign.service.order.OrderStatisticsService;
import com.university.webdesign.service.order.dto.ExportFormat;
import com.university.webdesign.service.order.dto.MonthlySummaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 订单统计服务实现。
 * <p>
 * 对应《对外方法表》3.2 的 `OrderStatisticsService`：
 * 个人月度消费统计（M2-09）与消费明细导出/打印（M2-10）。
 * 过滤规则与聚合口径都基于 `order_detail` 的下单快照，菜谱后续改名不影响历史统计。
 */
@Slf4j
@Service
@Transactional
public class OrderStatisticsServiceImpl implements OrderStatisticsService
{
	/**
	 * 有效订单状态集合
	 */
	private static final List<OrderStatus> ACTIVE_STATUSES = List.of(OrderStatus.PENDING, OrderStatus.VALID);

	private final OrderFormRepository orderFormRepository;

	public OrderStatisticsServiceImpl(OrderFormRepository orderFormRepository) {
		this.orderFormRepository = orderFormRepository;
	}

	@Override
	public MonthlySummaryVO monthlySummary(Long employeeId, YearMonth month) {
		UserContext context = UserContextHolder.require();
		Long target = employeeId == null ? context.userId() : employeeId;
		if (!Objects.equals(target, context.userId())
				&& !context.hasAnyRole(RoleCodes.MANAGER, RoleCodes.FINANCE)) {
			throw BusinessException.forbidden("只能查看自己的消费明细");
		}
		YearMonth target_month = month == null ? YearMonth.now() : month;
		List<OrderForm> orders = loadMonthlyOrders(target, target_month);

		MonthlySummaryVO result = new MonthlySummaryVO();
		result.setEmployeeId(target);
		result.setMonth(target_month.toString());
		result.setOrderCount(orders.size());

		BigDecimal totalAmount = BigDecimal.ZERO;
		Map<String, MonthlySummaryVO.CategoryBreakdownVO> categories = new LinkedHashMap<>();
		Map<Long, MonthlySummaryVO.DishSummaryVO> dishes = new LinkedHashMap<>();
		for (OrderForm order : orders) {
			if (order.getTotalAmount() != null) {
				totalAmount = totalAmount.add(order.getTotalAmount());
			}
			for (OrderDetail detail : order.getDetails()) {
				String category = detail.getCategory() == null || detail.getCategory().isBlank()
						? "未分类" : detail.getCategory();
				MonthlySummaryVO.CategoryBreakdownVO categoryVO =
						categories.computeIfAbsent(category, key -> {
							MonthlySummaryVO.CategoryBreakdownVO created =
									new MonthlySummaryVO.CategoryBreakdownVO();
							created.setCategory(key);
							return created;
						});
				categoryVO.setQuantity(categoryVO.getQuantity() + quantityOf(detail));
				categoryVO.setAmount(categoryVO.getAmount().add(detail.subtotal()));

				MonthlySummaryVO.DishSummaryVO dishVO = dishes.computeIfAbsent(detail.getRecipeId(), key -> {
					MonthlySummaryVO.DishSummaryVO created = new MonthlySummaryVO.DishSummaryVO();
					created.setRecipeId(key);
					created.setRecipeName(detail.getRecipeName());
					created.setCategory(category);
					return created;
				});
				dishVO.setQuantity(dishVO.getQuantity() + quantityOf(detail));
				dishVO.setAmount(dishVO.getAmount().add(detail.subtotal()));
			}
		}
		result.setTotalAmount(totalAmount);
		for (MonthlySummaryVO.CategoryBreakdownVO category : categories.values()) {
			if (totalAmount.signum() > 0) {
				category.setPercentage(category.getAmount()
						.multiply(BigDecimal.valueOf(100))
						.divide(totalAmount, 1, RoundingMode.HALF_UP));
			}
		}
		List<MonthlySummaryVO.CategoryBreakdownVO> categoryList = new ArrayList<>(categories.values());
		categoryList.sort(Comparator.comparing(MonthlySummaryVO.CategoryBreakdownVO::getAmount).reversed());
		result.setCategoryBreakdown(categoryList);
		List<MonthlySummaryVO.DishSummaryVO> dishList = new ArrayList<>(dishes.values());
		dishList.sort(Comparator.comparing(MonthlySummaryVO.DishSummaryVO::getAmount).reversed());
		result.setDishBreakdown(dishList);
		return result;
	}

	@Override
	public Resource exportHistory(Long employeeId, YearMonth month, ExportFormat format) {
		if (format == ExportFormat.PDF) {
			throw new BusinessException(ErrorCode.PARAM_INVALID,
					"当前环境未引入 PDF 生成库，请使用 format=XLSX 或 format=CSV");
		}
		MonthlySummaryVO summary = monthlySummary(employeeId, month);
		YearMonth targetMonth = month == null ? YearMonth.now() : month;
		List<OrderForm> orders = loadMonthlyOrders(summary.getEmployeeId(), targetMonth);

		List<String> headers = List.of("订单号", "就餐日期", "状态", "菜名", "分类", "单位", "单价", "数量", "小计", "订单金额", "下单时间");
		List<List<String>> rows = new ArrayList<>();
		for (OrderForm order : orders) {
			for (OrderDetail detail : order.getDetails()) {
				rows.add(List.of(
						nullSafe(order.getOrderNo()),
						order.getOrderDate() == null ? "" : order.getOrderDate().toString(),
						order.getStatus() == null ? "" : order.getStatus().getText(),
						nullSafe(detail.getRecipeName()),
						nullSafe(detail.getCategory()),
						nullSafe(detail.getUnit()),
						plain(detail.getUnitPrice()),
						String.valueOf(quantityOf(detail)),
						plain(detail.subtotal()),
						plain(order.getTotalAmount()),
						order.getCreatedAt() == null ? "" : order.getCreatedAt().toString()));
			}
		}
		rows.add(List.of("合计", "", "", "", "", "", "", "", "", plain(summary.getTotalAmount()), ""));
		String name = "consumption-" + summary.getEmployeeId() + "-" + targetMonth;
		log.info("导出员工 {} 的 {} 消费明细，共 {} 行，格式 {}", summary.getEmployeeId(), targetMonth, rows.size(), format);
		return format == ExportFormat.CSV
				? TabularExport.csv(headers, rows)
				: TabularExport.xlsx("消费明细", headers, rows);
	}

	// ------------------------------------------------------------------ 内部方法

	private List<OrderForm> loadMonthlyOrders(Long employeeId, YearMonth month) {
		LocalDateTime start = month.atDay(1).atStartOfDay();
		LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
		return orderFormRepository
				.findByEmployeeIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
						employeeId, start, end)
				.stream()
				.filter(order -> order.getStatus() != null && ACTIVE_STATUSES.contains(order.getStatus()))
				.toList();
	}

	private int quantityOf(OrderDetail detail) {
		return detail.getQuantity() == null ? 0 : detail.getQuantity();
	}

	private String plain(BigDecimal value) {
		return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	private String nullSafe(String value) {
		return value == null ? "" : value;
	}
}
