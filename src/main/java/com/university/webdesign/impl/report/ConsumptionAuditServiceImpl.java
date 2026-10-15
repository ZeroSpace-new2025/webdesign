package com.university.webdesign.impl.report;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.service.order.OrderQueryService;
import com.university.webdesign.service.order.dto.OrderDetailVO;
import com.university.webdesign.service.order.dto.OrderVO;
import com.university.webdesign.impl.report.support.MonthlyOrderReader;
import com.university.webdesign.service.report.ConsumptionAuditService;
import com.university.webdesign.service.report.MonthConverter;
import com.university.webdesign.service.report.dto.ConsumptionDetailVO;
import com.university.webdesign.service.report.dto.DeptConsumptionVO;
import com.university.webdesign.service.report.dto.DishSalesVO;
import com.university.webdesign.service.report.dto.EmployeeConsumptionVO;
import com.university.webdesign.service.user.UserService;
import com.university.webdesign.service.user.dto.UserBriefVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 员工消费审计服务实现。
 * <p>
 * 对应《对外方法表》5.2 的 {@code ConsumptionAuditService} 全部方法：
 * 员工月度订单汇总与消费明细、部门维度消费汇总。
 * <p>
 * 取数边界（硬性）：明细一律经 {@link OrderQueryService} 读取快照，
 * **不直接访问订单表**；部门归属经 {@link UserService#listByIds} 补齐。
 * <p>
 * 事务统一用普通 {@code @Transactional}（含查询），不加 {@code readOnly = true}。
 */
@Slf4j
@Service
public class ConsumptionAuditServiceImpl implements ConsumptionAuditService
{
	/**
	 * 单次返回的订单明细条数上限（对应 M4-25 `withDetails=true`）
	 */
	private static final int MAX_DETAILS = 100;

	private final MonthlyOrderReader orderReader;
	private final UserService userService;

	/**
	 * 构造器注入
	 *
	 * @param orderQueryService 订单取数契约（跨模块，只读）
	 * @param userService       员工信息契约（跨模块，只读）
	 */
	public ConsumptionAuditServiceImpl(OrderQueryService orderQueryService, UserService userService) {
		this.orderReader = new MonthlyOrderReader(orderQueryService);
		this.userService = userService;
	}

	@Override
	@Transactional
	public EmployeeConsumptionVO auditEmployee(Long employeeId, YearMonth month, boolean withDetails) {
		assertAuditPermission("员工消费审计");
		if (employeeId == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "员工ID不能为空");
		}
		if (month == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "月份不能为空");
		}
		UserBriefVO brief = userService.getBrief(employeeId);
		if (brief == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "员工不存在：" + employeeId);
		}
		Set<Long> employeeIds = new LinkedHashSet<>();
		employeeIds.add(employeeId);
		List<OrderVO> orders = orderReader.readMonth(month, employeeIds);
		EmployeeConsumptionVO vo = new EmployeeConsumptionVO();
		vo.setEmployeeId(employeeId);
		vo.setEmployeeNo(brief.getEmployeeNo());
		vo.setEmployeeName(brief.getName());
		vo.setDeptId(brief.getDeptId());
		vo.setDeptName(brief.getDeptName());
		vo.setWorkstation(brief.getWorkstation());
		vo.setMonth(MonthConverter.format(month));
		vo.setOrderCount((long) orders.size());
		long totalQuantity = 0L;
		BigDecimal totalAmount = BigDecimal.ZERO;
		List<ConsumptionDetailVO> details = new ArrayList<>();
		for (OrderVO order : orders) {
			List<DishSalesVO> items = itemsOf(order);
			totalAmount = totalAmount.add(order.getTotalAmount() == null
					? BigDecimal.ZERO
					: order.getTotalAmount());
			for (DishSalesVO item : items) {
				totalQuantity += item.getQuantity() == null ? 0L : item.getQuantity();
			}
			if (withDetails && details.size() < MAX_DETAILS) {
				details.add(toDetail(order, items));
			}
		}
		vo.setTotalQuantity(totalQuantity);
		vo.setTotalAmount(totalAmount);
		vo.setDetails(details);
		log.debug("员工消费审计：员工ID={}，月份={}，订单数={}，总额={}",
				employeeId, month, orders.size(), totalAmount);
		return vo;
	}

	@Override
	@Transactional
	public List<DeptConsumptionVO> sumByDept(YearMonth month, List<Long> deptIds) {
		assertAuditPermission("部门消费汇总");
		if (month == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "月份不能为空");
		}
		List<OrderVO> orders = orderReader.readMonth(month);
		if (orders.isEmpty()) {
			return List.of();
		}
		Set<Long> employeeIds = new LinkedHashSet<>();
		for (OrderVO order : orders) {
			if (order.getEmployeeId() != null) {
				employeeIds.add(order.getEmployeeId());
			}
		}
		Map<Long, UserBriefVO> briefs = new LinkedHashMap<>();
		for (UserBriefVO brief : userService.listByIds(employeeIds)) {
			if (brief != null && brief.getUserId() != null) {
				briefs.put(brief.getUserId(), brief);
			}
		}
		Set<Long> filter = deptIds == null ? Set.of() : new LinkedHashSet<>(deptIds);
		Map<Long, DeptAggregate> aggregates = new LinkedHashMap<>();
		for (OrderVO order : orders) {
			UserBriefVO brief = briefs.get(order.getEmployeeId());
			Long deptId = brief == null ? order.getDeptId() : brief.getDeptId();
			if (!filter.isEmpty() && (deptId == null || !filter.contains(deptId))) {
				continue;
			}
			DeptAggregate aggregate = aggregates.computeIfAbsent(deptId,
					ignored -> new DeptAggregate(deptId,
							brief == null ? order.getDeptName() : brief.getDeptName()));
			aggregate.orderCount++;
			aggregate.employeeIds.add(order.getEmployeeId());
			aggregate.totalAmount = aggregate.totalAmount.add(order.getTotalAmount() == null
					? BigDecimal.ZERO
					: order.getTotalAmount());
			for (DishSalesVO item : itemsOf(order)) {
				aggregate.totalQuantity += item.getQuantity() == null ? 0L : item.getQuantity();
			}
		}
		List<DeptConsumptionVO> result = new ArrayList<>();
		for (DeptAggregate aggregate : aggregates.values()) {
			DeptConsumptionVO vo = new DeptConsumptionVO();
			vo.setDeptId(aggregate.deptId);
			vo.setDeptName(aggregate.deptName);
			vo.setEmployeeCount((long) aggregate.employeeIds.size());
			vo.setOrderCount(aggregate.orderCount);
			vo.setTotalQuantity(aggregate.totalQuantity);
			vo.setTotalAmount(aggregate.totalAmount);
			result.add(vo);
		}
		return result;
	}

	/**
	 * 订单视图 → 明细视图
	 *
	 * @param order 订单视图
	 * @param items 菜品明细
	 * @return 明细视图
	 */
	private ConsumptionDetailVO toDetail(OrderVO order, List<DishSalesVO> items) {
		ConsumptionDetailVO detail = new ConsumptionDetailVO();
		detail.setOrderId(order.getOrderId());
		detail.setOrderNo(order.getOrderNo());
		detail.setOrderDate(order.getOrderDate() == null ? null : order.getOrderDate().toString());
		detail.setCreatedAt(order.getCreatedAt());
		detail.setStatus(order.getStatus());
		detail.setStatusText(order.getStatusText());
		detail.setTotalAmount(order.getTotalAmount());
		detail.setItems(items);
		return detail;
	}

	/**
	 * 订单视图 → 菜品明细（下单时刻快照）
	 *
	 * @param order 订单视图
	 * @return 菜品明细
	 */
	private List<DishSalesVO> itemsOf(OrderVO order) {
		List<DishSalesVO> items = new ArrayList<>();
		if (order == null || order.getItems() == null) {
			return items;
		}
		for (OrderDetailVO detail : order.getItems()) {
			DishSalesVO item = new DishSalesVO();
			item.setRecipeId(detail.getRecipeId());
			item.setRecipeName(detail.getRecipeName());
			item.setCategory(detail.getCategory());
			item.setQuantity(detail.getQuantity() == null ? 0L : detail.getQuantity().longValue());
			item.setAmount(detail.getAmount() == null ? BigDecimal.ZERO : detail.getAmount());
			items.add(item);
		}
		return items;
	}

	/**
	 * 校验审计权限：财务管理或餐厅经理（或持有审计/报表权限点）
	 *
	 * @param action 动作名称，用于中文提示
	 */
	private void assertAuditPermission(String action) {
		UserContext context = UserContextHolder.require();
		if (context.hasAnyRole(RoleCodes.FINANCE, RoleCodes.MANAGER)) {
			return;
		}
		if (context.hasPermission(PermissionEnum.AUDIT_VIEW) || context.hasPermission(PermissionEnum.REPORT_VIEW)) {
			return;
		}
		throw new BusinessException(ErrorCode.FORBIDDEN, "无权执行该操作：" + action);
	}

	/**
	 * 部门聚合中间结果
	 */
	private static final class DeptAggregate
	{
		/**
		 * 部门ID
		 */
		private final Long deptId;

		/**
		 * 部门名称
		 */
		private final String deptName;

		/**
		 * 该部门有消费的员工
		 */
		private final Set<Long> employeeIds = new LinkedHashSet<>();

		/**
		 * 订单数
		 */
		private long orderCount;

		/**
		 * 菜品总数量
		 */
		private long totalQuantity;

		/**
		 * 消费总金额
		 */
		private BigDecimal totalAmount = BigDecimal.ZERO;

		private DeptAggregate(Long deptId, String deptName) {
			this.deptId = deptId;
			this.deptName = deptName;
		}
	}
}
