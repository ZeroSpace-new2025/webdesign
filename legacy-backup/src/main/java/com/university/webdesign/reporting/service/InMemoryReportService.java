package com.university.webdesign.reporting.service;

import com.university.webdesign.reporting.api.DishSalesDTO;
import com.university.webdesign.reporting.api.EmployeeConsumptionDetailDTO;
import com.university.webdesign.reporting.api.EmployeeConsumptionReportDTO;
import com.university.webdesign.reporting.api.MonthlySalesReportDTO;
import com.university.webdesign.reporting.api.OrderItemSnapshotDTO;
import com.university.webdesign.user.api.UserDTO;
import com.university.webdesign.user.api.UserQueryData;
import com.university.webdesign.user.service.UserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存订单样本的财务报表服务。
 */
@Service
public class InMemoryReportService implements ReportService
{
	private final UserService userService;
	private final List<OrderRecord> orders = new ArrayList<>();
	private final Map<YearMonth, MonthlySalesReportDTO> monthlyCache = new ConcurrentHashMap<>();

	/**
	 * 注入用户服务，用于给消费报表补充姓名、部门和工位。
	 */
	public InMemoryReportService(UserService userService) {
		this.userService = userService;
		seedOrders();
	}

	@Override
	public MonthlySalesReportDTO getMonthlySalesReport(YearMonth month) {
		YearMonth target = month == null ? YearMonth.now() : month;
		return copyMonthly(monthlyCache.computeIfAbsent(target, this::buildMonthlyReport));
	}

	@Override
	public EmployeeConsumptionReportDTO getEmployeeConsumptionReport(Long userId, YearMonth month) {
		YearMonth target = month == null ? YearMonth.now() : month;
		return buildEmployeeReport(userService.getUserInfo(userId), target);
	}

	@Override
	public List<EmployeeConsumptionReportDTO> queryEmployeeConsumptionReports(YearMonth month) {
		YearMonth target = month == null ? YearMonth.now() : month;
		return userService.query(new UserQueryData()).stream()
			.map(user -> buildEmployeeReport(user, target))
			.sorted(Comparator.comparing(EmployeeConsumptionReportDTO::getTotalAmount).reversed())
			.toList();
	}

	@Override
	public List<YearMonth> getAvailableReportMonths() {
		return orders.stream()
			.map(order -> YearMonth.from(order.time))
			.distinct()
			.sorted(Comparator.reverseOrder())
			.toList();
	}

	@Override
	public void refreshMonthlyReport(YearMonth month) {
		YearMonth target = month == null ? YearMonth.now() : month;
		monthlyCache.put(target, buildMonthlyReport(target));
	}

	private MonthlySalesReportDTO buildMonthlyReport(YearMonth month) {
		// 餐厅月报按菜品ID聚合销量和金额，订单明细本身不直接返回。
		Map<Long, DishAccumulator> dishes = new LinkedHashMap<>();
		int orderCount = 0;
		long totalQuantity = 0;
		BigDecimal totalAmount = BigDecimal.ZERO;
		for (OrderRecord order : orders) {
			if (!YearMonth.from(order.time).equals(month)) {
				continue;
			}
			orderCount++;
			for (OrderLine line : order.items) {
				DishAccumulator accumulator = dishes.computeIfAbsent(line.recipeId,
					ignored -> new DishAccumulator(line.recipeId, line.recipeName, line.category));
				accumulator.quantity += line.quantity;
				accumulator.amount = accumulator.amount.add(line.amount());
				totalQuantity += line.quantity;
				totalAmount = totalAmount.add(line.amount());
			}
		}

		MonthlySalesReportDTO report = new MonthlySalesReportDTO();
		report.setMonth(month);
		report.setOrderCount(orderCount);
		report.setTotalQuantity(totalQuantity);
		report.setTotalAmount(totalAmount);
		report.setGeneratedTime(System.currentTimeMillis());
		report.setItems(dishes.values().stream()
			.map(accumulator -> {
				DishSalesDTO item = new DishSalesDTO();
				item.setRecipeId(accumulator.recipeId);
				item.setRecipeName(accumulator.recipeName);
				item.setCategory(accumulator.category);
				item.setQuantity(accumulator.quantity);
				item.setSalesAmount(accumulator.amount);
				return item;
			})
			.sorted(Comparator.comparing(DishSalesDTO::getSalesAmount).reversed())
			.toList());
		return report;
	}

	private EmployeeConsumptionReportDTO buildEmployeeReport(UserDTO user, YearMonth month) {
		// 员工审计保留每一笔订单和订单项快照，方便财务追溯。
		List<EmployeeConsumptionDetailDTO> details = new ArrayList<>();
		long totalQuantity = 0;
		BigDecimal totalAmount = BigDecimal.ZERO;
		for (OrderRecord order : orders) {
			if (!order.userId.equals(user.getUserId()) || !YearMonth.from(order.time).equals(month)) {
				continue;
			}
			EmployeeConsumptionDetailDTO detail = new EmployeeConsumptionDetailDTO();
			detail.setOrderId(order.orderId);
			detail.setCreatedTime(toTimestamp(order.time));
			detail.setTotalAmount(order.totalAmount());
			detail.setItems(order.items.stream().map(line -> {
				OrderItemSnapshotDTO item = new OrderItemSnapshotDTO();
				item.setRecipeId(line.recipeId);
				item.setRecipeName(line.recipeName);
				item.setUnitPrice(line.unitPrice);
				item.setQuantity(line.quantity);
				item.setAmount(line.amount());
				return item;
			}).toList());
			details.add(detail);
			totalAmount = totalAmount.add(detail.getTotalAmount());
			totalQuantity += detail.getItems().stream().mapToLong(OrderItemSnapshotDTO::getQuantity).sum();
		}
		details.sort(Comparator.comparing(EmployeeConsumptionDetailDTO::getCreatedTime).reversed());

		EmployeeConsumptionReportDTO report = new EmployeeConsumptionReportDTO();
		report.setUserId(user.getUserId());
		report.setEmployeeName(user.getName());
		report.setDepartment(user.getDepartment());
		report.setWorkstation(user.getWorkstation());
		report.setMonth(month);
		report.setOrderCount(details.size());
		report.setTotalQuantity(totalQuantity);
		report.setTotalAmount(totalAmount);
		report.setDetails(details);
		return report;
	}

	private MonthlySalesReportDTO copyMonthly(MonthlySalesReportDTO source) {
		MonthlySalesReportDTO copy = new MonthlySalesReportDTO();
		copy.setMonth(source.getMonth());
		copy.setOrderCount(source.getOrderCount());
		copy.setTotalQuantity(source.getTotalQuantity());
		copy.setTotalAmount(source.getTotalAmount());
		copy.setGeneratedTime(source.getGeneratedTime());
		copy.setItems(source.getItems() == null ? List.of() : source.getItems().stream().map(item -> {
			DishSalesDTO copyItem = new DishSalesDTO();
			copyItem.setRecipeId(item.getRecipeId());
			copyItem.setRecipeName(item.getRecipeName());
			copyItem.setCategory(item.getCategory());
			copyItem.setQuantity(item.getQuantity());
			copyItem.setSalesAmount(item.getSalesAmount());
			return copyItem;
		}).toList());
		return copy;
	}

	private void seedOrders() {
		// 使用当前月和上个月生成演示订单，确保月份选择器始终有数据。
		YearMonth current = YearMonth.now();
		YearMonth previous = current.minusMonths(1);
		addOrder(1001L, 5L, current.atDay(2).atTime(11, 32), List.of(
			line(101L, "黑椒牛柳", "热菜", "32.00", 1),
			line(201L, "米饭", "主食", "2.00", 1),
			line(301L, "例汤", "汤品", "4.00", 1)));
		addOrder(1002L, 6L, current.atDay(2).atTime(11, 35), List.of(
			line(102L, "宫保鸡丁", "热菜", "26.00", 1),
			line(201L, "米饭", "主食", "2.00", 1)));
		addOrder(1003L, 7L, current.atDay(3).atTime(11, 45), List.of(
			line(103L, "香煎鳕鱼", "热菜", "36.00", 1),
			line(202L, "杂粮饭", "主食", "3.00", 1),
			line(302L, "酸奶", "饮品", "6.00", 1)));
		addOrder(1004L, 8L, current.atDay(3).atTime(11, 48), List.of(
			line(104L, "青椒肉丝", "热菜", "22.00", 1),
			line(201L, "米饭", "主食", "2.00", 1)));
		addOrder(1005L, 5L, current.atDay(4).atTime(11, 30), List.of(
			line(103L, "香煎鳕鱼", "热菜", "36.00", 1),
			line(202L, "杂粮饭", "主食", "3.00", 1)));
		addOrder(1006L, 2L, current.atDay(4).atTime(11, 52), List.of(
			line(101L, "黑椒牛柳", "热菜", "32.00", 1),
			line(301L, "例汤", "汤品", "4.00", 1)));
		addOrder(1007L, 3L, current.atDay(5).atTime(11, 33), List.of(
			line(102L, "宫保鸡丁", "热菜", "26.00", 1),
			line(201L, "米饭", "主食", "2.00", 1)));

		addOrder(1101L, 5L, previous.atDay(8).atTime(11, 38), List.of(
			line(101L, "黑椒牛柳", "热菜", "32.00", 1),
			line(201L, "米饭", "主食", "2.00", 1)));
		addOrder(1102L, 6L, previous.atDay(9).atTime(11, 42), List.of(
			line(103L, "香煎鳕鱼", "热菜", "36.00", 1),
			line(202L, "杂粮饭", "主食", "3.00", 1),
			line(302L, "酸奶", "饮品", "6.00", 1)));
		addOrder(1103L, 7L, previous.atDay(10).atTime(11, 31), List.of(
			line(104L, "青椒肉丝", "热菜", "22.00", 1),
			line(201L, "米饭", "主食", "2.00", 1),
			line(301L, "例汤", "汤品", "4.00", 1)));
	}

	private void addOrder(Long orderId, Long userId, LocalDateTime time, List<OrderLine> items) {
		orders.add(new OrderRecord(orderId, userId, time, items));
	}

	private OrderLine line(Long recipeId, String recipeName, String category, String unitPrice, int quantity) {
		return new OrderLine(recipeId, recipeName, category, new BigDecimal(unitPrice), quantity);
	}

	private long toTimestamp(LocalDateTime time) {
		return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	private record OrderRecord(Long orderId, Long userId, LocalDateTime time, List<OrderLine> items)
	{
		private BigDecimal totalAmount() {
			return items.stream().map(OrderLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
		}
	}

	private record OrderLine(Long recipeId, String recipeName, String category, BigDecimal unitPrice, int quantity)
	{
		private BigDecimal amount() {
			return unitPrice.multiply(BigDecimal.valueOf(quantity));
		}
	}

	private static final class DishAccumulator
	{
		private final Long recipeId;
		private final String recipeName;
		private final String category;
		private long quantity;
		private BigDecimal amount = BigDecimal.ZERO;

		private DishAccumulator(Long recipeId, String recipeName, String category) {
			this.recipeId = recipeId;
			this.recipeName = recipeName;
			this.category = category;
		}
	}
}
