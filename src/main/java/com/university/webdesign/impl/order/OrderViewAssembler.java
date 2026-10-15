package com.university.webdesign.impl.order;

import com.university.webdesign.domain.order.OrderDetail;
import com.university.webdesign.domain.order.OrderForm;
import com.university.webdesign.service.order.dto.OrderBriefVO;
import com.university.webdesign.service.order.dto.OrderDetailVO;
import com.university.webdesign.service.order.dto.OrderVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单实体 → 视图对象的转换器。
 * <p>
 * 订单模块对外的读模型统一由这里产出，避免 `OrderServiceImpl` 与 `OrderQueryService` 各自拼一套。
 * 所有转换都在事务内调用，`order.details` 处于已加载状态。
 */
@Component
public class OrderViewAssembler
{
	/**
	 * 时间文案格式
	 */
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * 实体 → 订单视图（含明细快照）
	 *
	 * @param order 订单实体
	 * @return 订单视图；入参为 null 时返回 null
	 */
	public OrderVO toVO(OrderForm order) {
		if (order == null) {
			return null;
		}
		OrderVO vo = new OrderVO();
		vo.setOrderId(order.getOrderId());
		vo.setOrderNo(order.getOrderNo());
		vo.setEmployeeId(order.getEmployeeId());
		vo.setDeptId(order.getDeptId());
		vo.setOrderDate(order.getOrderDate());
		vo.setStatus(order.getStatus() == null ? null : order.getStatus().name());
		vo.setStatusText(order.getStatus() == null ? null : order.getStatus().getText());
		vo.setTotalAmount(order.getTotalAmount());
		vo.setRemark(order.getRemark());
		vo.setCreatedAt(format(order.getCreatedAt()));
		vo.setUpdatedAt(format(order.getUpdatedAt()));
		vo.setInvalidatedBy(order.getInvalidatedBy());
		vo.setInvalidateReason(order.getInvalidateReason());
		vo.setVersion(order.getVersion());
		List<OrderDetailVO> items = new ArrayList<>();
		for (OrderDetail detail : order.getDetails()) {
			items.add(toItemVO(detail));
		}
		vo.setItems(items);
		return vo;
	}

	/**
	 * 实体 → 精简视图（供 M3 聚合与配送取数）
	 *
	 * @param order 订单实体
	 * @return 精简视图；入参为 null 时返回 null
	 */
	public OrderBriefVO toBriefVO(OrderForm order) {
		if (order == null) {
			return null;
		}
		OrderBriefVO vo = new OrderBriefVO();
		vo.setOrderId(order.getOrderId());
		vo.setOrderNo(order.getOrderNo());
		vo.setEmployeeId(order.getEmployeeId());
		vo.setOrderDate(order.getOrderDate());
		vo.setTotalAmount(order.getTotalAmount());
		List<OrderDetailVO> items = new ArrayList<>();
		for (OrderDetail detail : order.getDetails()) {
			items.add(toItemVO(detail));
		}
		vo.setItems(items);
		return vo;
	}

	/**
	 * 明细实体 → 明细视图
	 *
	 * @param detail 明细实体
	 * @return 明细视图
	 */
	public OrderDetailVO toItemVO(OrderDetail detail) {
		OrderDetailVO vo = new OrderDetailVO();
		vo.setDetailId(detail.getDetailId());
		vo.setRecipeId(detail.getRecipeId());
		vo.setRecipeName(detail.getRecipeName());
		vo.setCategory(detail.getCategory());
		vo.setUnit(detail.getUnit());
		vo.setUnitPrice(detail.getUnitPrice());
		vo.setQuantity(detail.getQuantity());
		vo.setAmount(detail.subtotal());
		return vo;
	}

	/**
	 * 时间格式化
	 *
	 * @param time 时间，可为 null
	 * @return `yyyy-MM-dd HH:mm:ss`；入参为 null 时返回 null
	 */
	public String format(LocalDateTime time) {
		return time == null ? null : DATE_TIME.format(time);
	}
}
