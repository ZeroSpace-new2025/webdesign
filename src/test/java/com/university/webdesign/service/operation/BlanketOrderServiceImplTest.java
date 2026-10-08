package com.university.webdesign.service.operation;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.domain.operation.DailyStatistics;
import com.university.webdesign.domain.operation.DailyStatisticsItem;
import com.university.webdesign.repository.operation.DailyStatisticsRepository;
import com.university.webdesign.service.impl.operation.BlanketOrderServiceImpl;
import com.university.webdesign.service.menu.RecipeService;
import com.university.webdesign.service.operation.dto.CategoryStatVO;
import com.university.webdesign.service.operation.dto.CategorySumVO;
import com.university.webdesign.service.operation.dto.DailyStatVO;
import com.university.webdesign.service.operation.dto.PrintFormat;
import com.university.webdesign.service.operation.dto.ServiceWindowVO;
import com.university.webdesign.service.order.OrderQueryService;
import com.university.webdesign.service.order.dto.OrderBriefVO;
import com.university.webdesign.service.order.dto.OrderDetailVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 总括订单服务单元测试。
 * <p>
 * 用 Mockito 直接驱动 {@link BlanketOrderServiceImpl}：订单数据只经
 * {@code OrderQueryService.listValidByDate} 取得（不触达订单表），
 * 覆盖聚合落库、幂等、分类汇总与生产单的截止时间校验。
 */
class BlanketOrderServiceImplTest
{
	private final OrderQueryService orderQueryService = mock(OrderQueryService.class);

	private final RecipeService recipeService = mock(RecipeService.class);

	private final DailyStatisticsRepository dailyStatisticsRepository = mock(DailyStatisticsRepository.class);

	private final com.university.webdesign.service.operation.ServiceWindowService serviceWindowService =
			mock(com.university.webdesign.service.operation.ServiceWindowService.class);

	private final BlanketOrderServiceImpl service = new BlanketOrderServiceImpl(
			orderQueryService, recipeService, dailyStatisticsRepository, serviceWindowService);

	@Test
	@DisplayName("聚合：按 recipeId 汇总数量与金额并落库")
	void aggregateGroupsByRecipe() {
		LocalDate date = LocalDate.of(2026, 1, 5);
		when(orderQueryService.listValidByDate(date)).thenReturn(List.of(
				order(1L, detail(101L, "小炒肉", "热菜", "份", 2, "36.00"),
						detail(103L, "米饭", "主食", "两", 1, "2.00")),
				order(2L, detail(101L, "小炒肉", "热菜", "份", 1, "18.00"))));
		when(dailyStatisticsRepository.findByStatisticsDate(date)).thenReturn(Optional.empty());
		when(dailyStatisticsRepository.save(any(DailyStatistics.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		DailyStatVO vo = service.aggregate(date, false);

		assertThat(vo.getStatisticsDate()).isEqualTo(date);
		assertThat(vo.getTotalOrders()).isEqualTo(2);
		assertThat(vo.getTotalAmount()).isEqualByComparingTo("56.00");
		assertThat(vo.getItems()).hasSize(2);
		CategoryStatVO meat = vo.getItems().get(0);
		assertThat(meat.getRecipeId()).isEqualTo(101L);
		assertThat(meat.getRecipeName()).isEqualTo("小炒肉");
		assertThat(meat.getCategory()).isEqualTo("热菜");
		assertThat(meat.getUnit()).isEqualTo("份");
		assertThat(meat.getTotalQuantity()).isEqualTo(3);
		assertThat(meat.getTotalAmount()).isEqualByComparingTo("54.00");
		assertThat(meat.getOrderCount()).isEqualTo(2);
		assertThat(vo.getItems().get(1).getRecipeName()).isEqualTo("米饭");
	}

	@Test
	@DisplayName("聚合：force=false 且已有快照时直接复用，不重算")
	void aggregateIsIdempotentWithoutForce() {
		LocalDate date = LocalDate.of(2026, 1, 6);
		DailyStatistics existing = new DailyStatistics();
		existing.setId(9L);
		existing.setStatisticsDate(date);
		existing.setTotalOrders(1);
		existing.setTotalAmount(new BigDecimal("10.00"));
		existing.setStatus(DailyStatistics.STATUS_AGGREGATED);
		when(dailyStatisticsRepository.findByStatisticsDate(date)).thenReturn(Optional.of(existing));

		DailyStatVO vo = service.aggregate(date, false);

		assertThat(vo.getStatisticsId()).isEqualTo(9L);
		assertThat(vo.getTotalOrders()).isEqualTo(1);
		verify(orderQueryService, never()).listValidByDate(any());
	}

	@Test
	@DisplayName("聚合：force=true 时覆盖既有快照（原地覆盖写，不触发唯一约束冲突）")
	void aggregateWithForceOverwrites() {
		LocalDate date = LocalDate.of(2026, 1, 7);
		DailyStatistics existing = new DailyStatistics();
		existing.setId(11L);
		existing.setStatisticsDate(date);
		existing.addItem(new DailyStatisticsItem());
		when(dailyStatisticsRepository.findByStatisticsDate(date)).thenReturn(Optional.of(existing));
		when(orderQueryService.listValidByDate(date)).thenReturn(List.of(
				order(3L, detail(103L, "米饭", "主食", "两", 4, "8.00"))));
		when(dailyStatisticsRepository.save(any(DailyStatistics.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		DailyStatVO vo = service.aggregate(date, true);

		// 关键回归点：复用既有行而不是“先 delete 再 insert”。
		// Hibernate 会把 DELETE 推迟到 flush，而 INSERT 立即执行，
		// 同一事务内换行会撞 statistics_date 唯一约束（曾被全局异常处理器兜成 50000）。
		verify(dailyStatisticsRepository, never()).delete(any(DailyStatistics.class));
		ArgumentCaptor<DailyStatistics> captor = ArgumentCaptor.forClass(DailyStatistics.class);
		verify(dailyStatisticsRepository).save(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(11L);
		assertThat(captor.getValue().getStatisticsDate()).isEqualTo(date);
		assertThat(captor.getValue().getTotalOrders()).isEqualTo(1);
		// 明细被整体替换，不会叠加旧数据
		assertThat(captor.getValue().getItems()).hasSize(1);
		assertThat(vo.getTotalAmount()).isEqualByComparingTo("8.00");
	}

	@Test
	@DisplayName("查询总括订单：未聚合时返回空列表，不隐式写库")
	void listAggregatedReturnsEmptyWhenNoSnapshot() {
		LocalDate date = LocalDate.of(2026, 1, 8);
		when(dailyStatisticsRepository.findByStatisticsDate(date)).thenReturn(Optional.empty());

		assertThat(service.listAggregated(date, null)).isEmpty();
		verify(dailyStatisticsRepository, never()).save(any());
	}

	@Test
	@DisplayName("分类汇总：按分类合并菜品数、总量与金额")
	void sumByCategoryAggregates() {
		LocalDate date = LocalDate.of(2026, 1, 9);
		DailyStatistics snapshot = snapshot(date,
				item("热菜", 101L, "小炒肉", "份", 3, "54.00", 2),
				item("热菜", 102L, "红烧肉", "份", 1, "20.00", 1),
				item("主食", 103L, "米饭", "两", 5, "10.00", 3));
		when(dailyStatisticsRepository.findByStatisticsDate(date)).thenReturn(Optional.of(snapshot));

		List<CategorySumVO> sums = service.sumByCategory(date);

		assertThat(sums).hasSize(2);
		assertThat(sums.get(0).getCategory()).isEqualTo("热菜");
		assertThat(sums.get(0).getDishCount()).isEqualTo(2);
		assertThat(sums.get(0).getTotalQuantity()).isEqualTo(4);
		assertThat(sums.get(0).getTotalAmount()).isEqualByComparingTo("74.00");
		assertThat(sums.get(1).getCategory()).isEqualTo("主食");
	}

	@Test
	@DisplayName("生产单：未过截止时间时抛 42201")
	void printRejectsBeforeCutoff() {
		LocalDate today = LocalDate.now();
		when(serviceWindowService.get(today, null, null)).thenReturn(window(LocalTime.of(23, 59)));

		assertThatThrownBy(() -> service.printProductionOrder(today, null, PrintFormat.TXT))
				.isInstanceOf(BusinessException.class)
				.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
						.isEqualTo(ErrorCode.OUT_OF_TIME_WINDOW));
	}

	@Test
	@DisplayName("生产单：已过截止时间时输出纯文本内容")
	void printProducesText() throws Exception {
		LocalDate today = LocalDate.now();
		when(serviceWindowService.get(today, null, null)).thenReturn(window(LocalTime.of(0, 1)));
		when(dailyStatisticsRepository.findByStatisticsDate(today)).thenReturn(Optional.of(snapshot(today,
				item("热菜", 101L, "小炒肉", "份", 3, "54.00", 2))));

		Resource resource = service.printProductionOrder(today, null, PrintFormat.TXT);

		assertThat(resource.getFilename()).endsWith(".txt");
		String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		assertThat(content).contains("企业餐厅生产单").contains("小炒肉").contains("3份");
	}

	@Test
	@DisplayName("生产单：PDF 未实现时抛 40001")
	void printRejectsPdf() {
		LocalDate today = LocalDate.now();
		when(serviceWindowService.get(today, null, null)).thenReturn(window(LocalTime.of(0, 1)));
		when(dailyStatisticsRepository.save(any(DailyStatistics.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		assertThatThrownBy(() -> service.printProductionOrder(today, null, PrintFormat.PDF))
				.isInstanceOf(BusinessException.class)
				.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
						.isEqualTo(ErrorCode.PARAM_INVALID));
	}

	// ------------------------------------------------------------------ 测试数据

	private OrderBriefVO order(Long orderId, OrderDetailVO... details) {
		OrderBriefVO brief = new OrderBriefVO();
		brief.setOrderId(orderId);
		brief.setOrderNo("20260105000" + orderId);
		brief.setEmployeeId(1000L + orderId);
		brief.setOrderDate(LocalDate.of(2026, 1, 5));
		BigDecimal total = BigDecimal.ZERO;
		for (OrderDetailVO detail : details) {
			total = total.add(detail.getAmount());
		}
		brief.setItems(new ArrayList<>(List.of(details)));
		brief.setTotalAmount(total);
		return brief;
	}

	private OrderDetailVO detail(Long recipeId, String name, String category, String unit,
			int quantity, String amount) {
		OrderDetailVO detail = new OrderDetailVO();
		detail.setRecipeId(recipeId);
		detail.setRecipeName(name);
		detail.setCategory(category);
		detail.setUnit(unit);
		detail.setQuantity(quantity);
		detail.setAmount(new BigDecimal(amount));
		return detail;
	}

	private DailyStatistics snapshot(LocalDate date, DailyStatisticsItem... items) {
		DailyStatistics snapshot = new DailyStatistics();
		snapshot.setId(1L);
		snapshot.setStatisticsDate(date);
		snapshot.setStatus(DailyStatistics.STATUS_AGGREGATED);
		snapshot.setTotalOrders(items.length);
		BigDecimal total = BigDecimal.ZERO;
		for (DailyStatisticsItem item : items) {
			snapshot.addItem(item);
			total = total.add(item.getAmount());
		}
		snapshot.setTotalAmount(total);
		return snapshot;
	}

	private DailyStatisticsItem item(String category, Long recipeId, String name, String unit,
			int quantity, String amount, int orderCount) {
		return new DailyStatisticsItem(category, recipeId, name, unit, quantity, new BigDecimal(amount), orderCount);
	}

	private ServiceWindowVO window(LocalTime cutoff) {
		ServiceWindowVO vo = new ServiceWindowVO();
		vo.setCutoffTime(cutoff);
		vo.setDeliveryStartTime(cutoff);
		return vo;
	}
}
