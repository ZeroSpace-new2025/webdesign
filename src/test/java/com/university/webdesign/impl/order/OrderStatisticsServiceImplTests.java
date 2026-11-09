package com.university.webdesign.impl.order;

import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.domain.order.OrderDetail;
import com.university.webdesign.domain.order.OrderForm;
import com.university.webdesign.domain.order.OrderStatus;
import com.university.webdesign.repository.order.OrderFormRepository;
import com.university.webdesign.service.order.dto.MonthlySummaryVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 个人月度消费统计口径测试。
 */
class OrderStatisticsServiceImplTests
{
	/**
	 * 避免 ThreadLocal 在测试线程中串号
	 */
	@AfterEach
	void clearContext() {
		UserContextHolder.clear();
	}

	/**
	 * 月度汇总按就餐日期取数，并排除已取消订单
	 */
	@Test
	void monthlySummaryUsesOrderDateAndOnlyCountsActiveOrders() {
		OrderFormRepository repository = mock(OrderFormRepository.class);
		YearMonth month = YearMonth.of(2026, 11);
		LocalDate start = month.atDay(1);
		LocalDate end = month.plusMonths(1).atDay(1);

		OrderForm active = new OrderForm();
		active.setOrderDate(LocalDate.of(2026, 11, 5));
		active.setStatus(OrderStatus.VALID);
		active.setTotalAmount(new BigDecimal("12.00"));
		OrderDetail detail = new OrderDetail();
		detail.setRecipeId(1L);
		detail.setRecipeName("红烧肉");
		detail.setCategory("热菜");
		detail.setUnitPrice(new BigDecimal("12.00"));
		detail.setQuantity(1);
		active.addDetail(detail);

		OrderForm cancelled = new OrderForm();
		cancelled.setOrderDate(LocalDate.of(2026, 11, 6));
		cancelled.setStatus(OrderStatus.CANCELLED);
		cancelled.setTotalAmount(new BigDecimal("20.00"));

		when(repository.findByEmployeeIdAndOrderDateGreaterThanEqualAndOrderDateLessThanOrderByOrderDateAscCreatedAtAsc(
				7L, start, end)).thenReturn(List.of(active, cancelled));
		UserContextHolder.set(new UserContext(7L, "E007", "张三", 1L, "R-07",
				"13800000007", List.of(RoleCodes.EMPLOYEE), List.of()));

		OrderStatisticsServiceImpl service = new OrderStatisticsServiceImpl(repository);
		MonthlySummaryVO summary = service.monthlySummary(7L, month);

		assertEquals(1, summary.getOrderCount());
		assertEquals(new BigDecimal("12.00"), summary.getTotalAmount());
		verify(repository)
				.findByEmployeeIdAndOrderDateGreaterThanEqualAndOrderDateLessThanOrderByOrderDateAscCreatedAtAsc(
						7L, start, end);
	}
}
