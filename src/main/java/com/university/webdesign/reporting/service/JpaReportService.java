package com.university.webdesign.reporting.service;

import com.university.webdesign.reporting.api.DishSalesDTO;
import com.university.webdesign.reporting.api.EmployeeConsumptionDetailDTO;
import com.university.webdesign.reporting.api.EmployeeConsumptionReportDTO;
import com.university.webdesign.reporting.api.MonthlySalesReportDTO;
import com.university.webdesign.reporting.api.OrderItemSnapshotDTO;
import com.university.webdesign.reporting.data.MonthlyReportEntity;
import com.university.webdesign.reporting.data.MonthlyReportItemEntity;
import com.university.webdesign.reporting.data.ReportOrderEntity;
import com.university.webdesign.reporting.data.ReportOrderItemEntity;
import com.university.webdesign.reporting.repository.MonthlyReportRepository;
import com.university.webdesign.reporting.repository.ReportOrderRepository;
import com.university.webdesign.user.api.UserDTO;
import com.university.webdesign.user.data.UserEntity;
import com.university.webdesign.user.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用JPA订单快照计算报表，并将月度汇总持久化到monthly_report表。
 */
@Service
@ConditionalOnProperty(name = "app.storage", havingValue = "database", matchIfMissing = true)
public class JpaReportService implements ReportService
{
	private final UserRepository userRepository;
	private final ReportOrderRepository reportOrderRepository;
	private final MonthlyReportRepository monthlyReportRepository;

	public JpaReportService(
		UserRepository userRepository,
		ReportOrderRepository reportOrderRepository,
		MonthlyReportRepository monthlyReportRepository) {
		this.userRepository = userRepository;
		this.reportOrderRepository = reportOrderRepository;
		this.monthlyReportRepository = monthlyReportRepository;
	}

	@Override
	@Transactional
	public MonthlySalesReportDTO getMonthlySalesReport(YearMonth month) {
		YearMonth target = month == null ? YearMonth.now() : month;
		return monthlyReportRepository.findByReportMonth(target.toString())
			.map(this::toDto)
			.orElseGet(() -> {
				refreshMonthlyReport(target);
				return monthlyReportRepository.findByReportMonth(target.toString())
					.map(this::toDto)
					.orElseThrow(() -> new IllegalStateException("月度报表生成失败"));
			});
	}

	@Override
	@Transactional(readOnly = true)
	public EmployeeConsumptionReportDTO getEmployeeConsumptionReport(Long userId, YearMonth month) {
		UserEntity user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("用户不存在"));
		return buildEmployeeReport(user, month == null ? YearMonth.now() : month);
	}

	@Override
	@Transactional(readOnly = true)
	public List<EmployeeConsumptionReportDTO> queryEmployeeConsumptionReports(YearMonth month) {
		YearMonth target = month == null ? YearMonth.now() : month;
		return userRepository.findAll().stream()
			.map(user -> buildEmployeeReport(user, target))
			.sorted(Comparator.comparing(EmployeeConsumptionReportDTO::getTotalAmount).reversed())
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<YearMonth> getAvailableReportMonths() {
		return reportOrderRepository.findAllByOrderByCreatedAtAsc().stream()
			.map(order -> YearMonth.from(order.getCreatedAt()))
			.distinct()
			.sorted(Comparator.reverseOrder())
			.toList();
	}

	@Override
	@Transactional
	public void refreshMonthlyReport(YearMonth month) {
		YearMonth target = month == null ? YearMonth.now() : month;
		MonthlySalesReportDTO calculated = buildMonthlyReport(target);
		MonthlyReportEntity entity = monthlyReportRepository.findByReportMonth(target.toString())
			.orElseGet(MonthlyReportEntity::new);
		entity.setReportMonth(target.toString());
		entity.setOrderCount(calculated.getOrderCount());
		entity.setTotalQuantity(calculated.getTotalQuantity());
		entity.setTotalAmount(calculated.getTotalAmount());
		entity.setGeneratedTime(calculated.getGeneratedTime());
		entity.getItems().clear();
		for (DishSalesDTO item : calculated.getItems()) {
			MonthlyReportItemEntity itemEntity = new MonthlyReportItemEntity();
			itemEntity.setReport(entity);
			itemEntity.setRecipeId(item.getRecipeId());
			itemEntity.setRecipeName(item.getRecipeName());
			itemEntity.setCategory(item.getCategory());
			itemEntity.setQuantity(item.getQuantity());
			itemEntity.setSalesAmount(item.getSalesAmount());
			entity.getItems().add(itemEntity);
		}
		monthlyReportRepository.save(entity);
	}

	private MonthlySalesReportDTO buildMonthlyReport(YearMonth month) {
		LocalDateTime start = month.atDay(1).atStartOfDay();
		LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
		List<ReportOrderEntity> orders = reportOrderRepository.findForPeriod(start, end);

		Map<Long, DishAccumulator> dishes = new LinkedHashMap<>();
		long totalQuantity = 0;
		BigDecimal totalAmount = BigDecimal.ZERO;
		for (ReportOrderEntity order : orders) {
			for (ReportOrderItemEntity item : order.getItems()) {
				DishAccumulator accumulator = dishes.computeIfAbsent(item.getRecipeId(),
					ignored -> new DishAccumulator(item.getRecipeId(), item.getRecipeName(), item.getCategory()));
				accumulator.quantity += item.getQuantity();
				accumulator.amount = accumulator.amount.add(item.getAmount());
				totalQuantity += item.getQuantity();
				totalAmount = totalAmount.add(item.getAmount());
			}
		}

		MonthlySalesReportDTO report = new MonthlySalesReportDTO();
		report.setMonth(month);
		report.setOrderCount(orders.size());
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

	private EmployeeConsumptionReportDTO buildEmployeeReport(UserEntity user, YearMonth month) {
		LocalDateTime start = month.atDay(1).atStartOfDay();
		LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
		List<ReportOrderEntity> orders = reportOrderRepository.findForUserAndPeriod(user.getId(), start, end);
		List<EmployeeConsumptionDetailDTO> details = new ArrayList<>();
		long totalQuantity = 0;
		BigDecimal totalAmount = BigDecimal.ZERO;
		for (ReportOrderEntity order : orders) {
			EmployeeConsumptionDetailDTO detail = new EmployeeConsumptionDetailDTO();
			detail.setOrderId(order.getId());
			detail.setCreatedTime(order.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
			detail.setTotalAmount(order.getTotalAmount());
			detail.setItems(order.getItems().stream().map(this::toDto).toList());
			details.add(detail);
			totalAmount = totalAmount.add(order.getTotalAmount());
			totalQuantity += order.getItems().stream().mapToLong(ReportOrderItemEntity::getQuantity).sum();
		}
		details.sort(Comparator.comparing(EmployeeConsumptionDetailDTO::getCreatedTime).reversed());

		EmployeeConsumptionReportDTO report = new EmployeeConsumptionReportDTO();
		report.setUserId(user.getId());
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

	private MonthlySalesReportDTO toDto(MonthlyReportEntity entity) {
		MonthlySalesReportDTO dto = new MonthlySalesReportDTO();
		dto.setMonth(YearMonth.parse(entity.getReportMonth()));
		dto.setOrderCount(entity.getOrderCount());
		dto.setTotalQuantity(entity.getTotalQuantity());
		dto.setTotalAmount(entity.getTotalAmount());
		dto.setGeneratedTime(entity.getGeneratedTime());
		dto.setItems(entity.getItems().stream().map(item -> {
			DishSalesDTO dtoItem = new DishSalesDTO();
			dtoItem.setRecipeId(item.getRecipeId());
			dtoItem.setRecipeName(item.getRecipeName());
			dtoItem.setCategory(item.getCategory());
			dtoItem.setQuantity(item.getQuantity());
			dtoItem.setSalesAmount(item.getSalesAmount());
			return dtoItem;
		}).toList());
		return dto;
	}

	private OrderItemSnapshotDTO toDto(ReportOrderItemEntity item) {
		OrderItemSnapshotDTO dto = new OrderItemSnapshotDTO();
		dto.setRecipeId(item.getRecipeId());
		dto.setRecipeName(item.getRecipeName());
		dto.setUnitPrice(item.getUnitPrice());
		dto.setQuantity(item.getQuantity());
		dto.setAmount(item.getAmount());
		return dto;
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
