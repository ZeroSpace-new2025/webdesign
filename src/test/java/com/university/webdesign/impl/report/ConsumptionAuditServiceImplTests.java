package com.university.webdesign.impl.report;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.service.order.OrderQueryService;
import com.university.webdesign.service.order.dto.OrderQuery;
import com.university.webdesign.service.report.dto.EmployeeConsumptionVO;
import com.university.webdesign.service.user.UserService;
import com.university.webdesign.service.user.dto.UserBriefVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 员工消费审计的本人访问边界测试。
 */
class ConsumptionAuditServiceImplTests
{
	/**
	 * 避免 ThreadLocal 在测试线程中串号
	 */
	@AfterEach
	void clearContext() {
		UserContextHolder.clear();
	}

	/**
	 * 普通员工可以查看自己的月度消费报表，无需 audit:view
	 */
	@Test
	void employeeCanViewOwnMonthlyReport() {
		OrderQueryService orderQueryService = mock(OrderQueryService.class);
		UserService userService = mock(UserService.class);
		when(orderQueryService.page(any(OrderQuery.class))).thenReturn(PageResult.empty());
		when(userService.getBrief(7L)).thenReturn(
				new UserBriefVO(7L, "E007", "张三", 1L, "研发部", "R-07", "13800000007"));
		UserContextHolder.set(new UserContext(7L, "E007", "张三", 1L, "R-07",
				"13800000007", List.of(RoleCodes.EMPLOYEE),
				List.of(PermissionEnum.ORDER_SUBMIT.getPermCode())));

		ConsumptionAuditServiceImpl service = new ConsumptionAuditServiceImpl(orderQueryService, userService);
		EmployeeConsumptionVO report = service.auditEmployee(7L, YearMonth.of(2026, 11), true);

		assertEquals(7L, report.getEmployeeId());
		assertEquals("张三", report.getEmployeeName());
		assertEquals(0L, report.getOrderCount());
	}

	/**
	 * 普通员工不能通过员工ID越权查看他人报表
	 */
	@Test
	void employeeCannotViewAnotherEmployeesMonthlyReport() {
		ConsumptionAuditServiceImpl service = new ConsumptionAuditServiceImpl(
				mock(OrderQueryService.class), mock(UserService.class));
		UserContextHolder.set(new UserContext(7L, "E007", "张三", 1L, "R-07",
				"13800000007", List.of(RoleCodes.EMPLOYEE),
				List.of(PermissionEnum.ORDER_SUBMIT.getPermCode())));

		assertThrows(BusinessException.class,
				() -> service.auditEmployee(8L, YearMonth.of(2026, 11), true));
	}

	/**
	 * 仅持有 report:view 的厨房主管不能查看员工消费明细
	 */
	@Test
	void reportViewPermissionAloneCannotViewAnotherEmployeesMonthlyReport() {
		ConsumptionAuditServiceImpl service = new ConsumptionAuditServiceImpl(
				mock(OrderQueryService.class), mock(UserService.class));
		UserContextHolder.set(new UserContext(3L, "K001", "陈主管", 3L, "K-01",
				"13800000003", List.of(RoleCodes.KITCHEN_SUPERVISOR),
				List.of(PermissionEnum.REPORT_VIEW.getPermCode())));

		assertThrows(BusinessException.class,
				() -> service.auditEmployee(5L, YearMonth.of(2026, 11), true));
	}

	/**
	 * 财务可以查看任意员工的月度消费报表
	 */
	@Test
	void financeCanViewAnotherEmployeesMonthlyReport() {
		OrderQueryService orderQueryService = mock(OrderQueryService.class);
		UserService userService = mock(UserService.class);
		when(orderQueryService.page(any(OrderQuery.class))).thenReturn(PageResult.empty());
		when(userService.getBrief(5L)).thenReturn(
				new UserBriefVO(5L, "E005", "张三", 1L, "研发部", "R-12", "13800000005"));
		UserContextHolder.set(new UserContext(2L, "F001", "周财务", 2L, "B-03",
				"13800000002", List.of(RoleCodes.FINANCE),
				List.of(PermissionEnum.AUDIT_VIEW.getPermCode())));

		ConsumptionAuditServiceImpl service = new ConsumptionAuditServiceImpl(orderQueryService, userService);
		EmployeeConsumptionVO report = service.auditEmployee(5L, YearMonth.of(2026, 11), true);

		assertEquals("张三", report.getEmployeeName());
		assertEquals(0L, report.getOrderCount());
	}
}
