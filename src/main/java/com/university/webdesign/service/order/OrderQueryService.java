package com.university.webdesign.service.order;

import com.university.webdesign.common.DateRange;
import com.university.webdesign.common.PageQuery;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.service.order.dto.OrderBriefVO;
import com.university.webdesign.service.order.dto.OrderQuery;
import com.university.webdesign.service.order.dto.OrderVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 订单与交易核心——查询服务。
 * <p>
 * 对应《对外方法表》3.2 的 `OrderQueryService`。本接口是**跨模块取数契约**：
 * M3（聚合总括订单、生成配送单）与 M4（月度报表、消费审计）都通过它拿订单数据，
 * 不允许任何模块直接访问 `order_form` / `order_detail` 表。
 * <p>
 * 数据归属校验在本层完成：本人或经理/财务，否则 40300。
 */
public interface OrderQueryService
{
	/**
	 * 查询订单详情（M2-06）
	 *
	 * @param orderId 订单ID
	 * @return 订单视图（含快照明细）
	 */
	OrderVO getDetail(Long orderId);

	/**
	 * 分页查询订单（M2-07）
	 *
	 * @param query 查询条件
	 * @return 分页订单
	 */
	PageResult<OrderVO> page(OrderQuery query);

	/**
	 * 个人历史分页（M2-08）
	 *
	 * @param employeeId 员工ID
	 * @param range      日期区间，可为 {@link DateRange#unbounded()}
	 * @param page       分页参数
	 * @return 分页订单
	 */
	PageResult<OrderVO> pageHistory(Long employeeId, DateRange range, PageQuery page);

	/**
	 * 查询某日全部有效订单（供 M3 聚合总括订单 / 生成配送单）
	 *
	 * @param date 就餐日期
	 * @return 有效订单精简视图（只含 {@code PENDING}/{@code VALID}）
	 */
	List<OrderBriefVO> listValidByDate(LocalDate date);
}
