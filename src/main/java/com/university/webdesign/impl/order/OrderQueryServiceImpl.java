package com.university.webdesign.impl.order;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.DateRange;
import com.university.webdesign.common.PageQuery;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.domain.order.OrderForm;
import com.university.webdesign.domain.order.OrderStatus;
import com.university.webdesign.repository.order.OrderFormRepository;
import com.university.webdesign.service.order.OrderQueryService;
import com.university.webdesign.service.order.dto.OrderBriefVO;
import com.university.webdesign.service.order.dto.OrderQuery;
import com.university.webdesign.service.order.dto.OrderVO;
import com.university.webdesign.service.user.UserService;
import com.university.webdesign.service.user.dto.UserBriefVO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 订单查询服务实现。
 * <p>
 * 对应《对外方法表》3.2 的 `OrderQueryService`，是 M3 / M4 取订单数据的唯一入口。
 * 归属校验：本人可查自己；{@code MANAGER} / {@code FINANCE} 可查他人，越权抛 40300。
 * <p>
 * 注意：本类统一使用普通 {@code @Transactional}，**不加 {@code readOnly = true}**，
 * 避免 Hibernate 把 flush 模式切成 MANUAL 导致同一事务内“先改后查”读到旧数据。
 */
@Service
@Transactional
public class OrderQueryServiceImpl implements OrderQueryService
{
	/**
	 * 有效订单状态集合
	 */
	private static final Collection<OrderStatus> ACTIVE_STATUSES = List.of(OrderStatus.PENDING, OrderStatus.VALID);

	private final OrderFormRepository orderFormRepository;
	private final OrderViewAssembler assembler;
	private final UserService userService;

	public OrderQueryServiceImpl(
			OrderFormRepository orderFormRepository,
			OrderViewAssembler assembler,
			UserService userService) {
		this.orderFormRepository = orderFormRepository;
		this.assembler = assembler;
		this.userService = userService;
	}

	@Override
	public OrderVO getDetail(Long orderId) {
		OrderForm order = requireOrder(orderId);
		UserContext context = UserContextHolder.require();
		boolean self = Objects.equals(order.getEmployeeId(), context.userId());
		if (!self && !canViewOthers(context)) {
			throw BusinessException.forbidden("只能查看自己的订单");
		}
		OrderVO vo = assembler.toVO(order);
		enrichEmployee(List.of(vo));
		return vo;
	}

	@Override
	public PageResult<OrderVO> page(OrderQuery query) {
		UserContext context = UserContextHolder.require();
		OrderQuery effective = query == null ? new OrderQuery() : query;
		if (!canViewOthers(context)) {
			// 员工强制只能查自己，忽略请求里伪造的 employeeId
			effective.setEmployeeId(context.userId());
		}
		Page<OrderForm> page = orderFormRepository.findAll(
				buildSpecification(effective),
				PageRequest.of(effective.toSpringPageNumber(), effective.normalizedPageSize(),
						Sort.by(Sort.Direction.DESC, "createdAt")));
		return toPageResult(page, effective.normalizedPageNum(), effective.normalizedPageSize());
	}

	@Override
	public PageResult<OrderVO> pageHistory(Long employeeId, DateRange range, PageQuery pageQuery) {
		UserContext context = UserContextHolder.require();
		Long target = employeeId == null ? context.userId() : employeeId;
		if (!Objects.equals(target, context.userId()) && !canViewOthers(context)) {
			throw BusinessException.forbidden("只能查看自己的历史订单");
		}
		DateRange effective = range == null ? DateRange.unbounded() : range;
		OrderQuery query = new OrderQuery();
		query.setEmployeeId(target);
		query.setDateFrom(effective.from());
		query.setDateTo(effective.to());
		query.setIncludeInactive(Boolean.TRUE);
		PageQuery paging = pageQuery == null ? new PageQuery() : pageQuery;
		query.setPageNum(paging.getPageNum());
		query.setPageSize(paging.getPageSize());

		Page<OrderForm> page = orderFormRepository.findAll(
				buildSpecification(query),
				PageRequest.of(query.toSpringPageNumber(), query.normalizedPageSize(),
						Sort.by(Sort.Direction.DESC, "createdAt")));
		return toPageResult(page, query.normalizedPageNum(), query.normalizedPageSize());
	}

	@Override
	public List<OrderBriefVO> listValidByDate(LocalDate date) {
		LocalDate target = date == null ? LocalDate.now() : date;
		List<OrderForm> orders = orderFormRepository.findByOrderDateAndStatusInOrderByCreatedAtAsc(
				target, ACTIVE_STATUSES);
		List<OrderBriefVO> result = new ArrayList<>();
		for (OrderForm order : orders) {
			result.add(assembler.toBriefVO(order));
		}
		return result;
	}

	// ------------------------------------------------------------------ 内部方法

	private boolean canViewOthers(UserContext context) {
		return context.hasAnyRole(RoleCodes.MANAGER, RoleCodes.FINANCE);
	}

	private OrderForm requireOrder(Long orderId) {
		if (orderId == null) {
			throw BusinessException.paramInvalid("订单ID不能为空");
		}
		return orderFormRepository.findById(orderId)
				.orElseThrow(() -> BusinessException.notFound("订单不存在：" + orderId));
	}

	private PageResult<OrderVO> toPageResult(Page<OrderForm> page, int pageNum, int pageSize) {
		List<OrderVO> list = new ArrayList<>();
		for (OrderForm order : page.getContent()) {
			list.add(assembler.toVO(order));
		}
		enrichEmployee(list);
		return new PageResult<>(page.getTotalElements(), list);
	}

	/**
	 * 批量补齐员工姓名与部门，避免逐条查询造成 N+1
	 *
	 * @param orders 订单视图列表
	 */
	private void enrichEmployee(List<OrderVO> orders) {
		Set<Long> ids = new LinkedHashSet<>();
		for (OrderVO order : orders) {
			if (order.getEmployeeId() != null) {
				ids.add(order.getEmployeeId());
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		Map<Long, UserBriefVO> briefs = new LinkedHashMap<>();
		for (UserBriefVO brief : userService.listByIds(ids)) {
			briefs.put(brief.getUserId(), brief);
		}
		for (OrderVO order : orders) {
			UserBriefVO brief = briefs.get(order.getEmployeeId());
			if (brief == null) {
				continue;
			}
			order.setEmployeeName(brief.getName());
			if (order.getDeptId() == null) {
				order.setDeptId(brief.getDeptId());
			}
			order.setDeptName(brief.getDeptName());
		}
	}

	/**
	 * 组装动态查询条件
	 *
	 * @param query 查询条件
	 * @return JPA Specification
	 */
	private Specification<OrderForm> buildSpecification(OrderQuery query) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (query.getEmployeeId() != null) {
				predicates.add(criteriaBuilder.equal(root.get("employeeId"), query.getEmployeeId()));
			}
			if (query.getDeptId() != null) {
				predicates.add(criteriaBuilder.equal(root.get("deptId"), query.getDeptId()));
			}
			if (query.getDateFrom() != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("orderDate"), query.getDateFrom()));
			}
			if (query.getDateTo() != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("orderDate"), query.getDateTo()));
			}
			if (query.getStatus() != null && !query.getStatus().isBlank()) {
				predicates.add(criteriaBuilder.equal(root.get("status"), OrderStatus.parse(query.getStatus())));
			} else if (!Boolean.TRUE.equals(query.getIncludeInactive())) {
				// 默认只看有效订单：已取消/已作废不占用产能，也不参与统计
				predicates.add(root.get("status").in(ACTIVE_STATUSES));
			}
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
}
