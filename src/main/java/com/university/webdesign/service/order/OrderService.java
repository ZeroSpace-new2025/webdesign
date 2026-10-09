package com.university.webdesign.service.order;

import com.university.webdesign.service.order.dto.MonthlySummaryVO;
import com.university.webdesign.service.order.dto.OrderModifyCmd;
import com.university.webdesign.service.order.dto.OrderHistoryQuery;
import com.university.webdesign.service.order.dto.OrderQuery;
import com.university.webdesign.service.order.dto.OrderSubmitCmd;
import com.university.webdesign.service.order.dto.OrderVO;
import com.university.webdesign.service.order.dto.ServiceWindowVO;
import com.university.webdesign.common.PageResult;

import java.time.LocalDate;

/**
 * 订单与交易核心——下单与生命周期服务。
 * <p>
 * 对应《对外方法表》3.2 的 `OrderService`。业务规则集中在本接口实现中：
 * <ul>
 *     <li><b>时间窗口</b>：当日订单必须在“订餐截止时间”前提交，截止后禁止新增/修改（42201）；</li>
 *     <li><b>一人一天一单</b>：同一员工同一天只能有一张有效订单（42202）；</li>
 *     <li><b>数据快照</b>：明细冗余保存下单时的菜名、分类、单位、单价，菜谱变动不影响历史订单；</li>
 *     <li><b>删单留痕</b>：经理作废违规订单置为 {@code INVALID} 并写审计字段，不做物理删除。</li>
 * </ul>
 * 截止时间唯一来源是 {@code ServiceWindowService.get(date, scope, deptId)}，
 * 订单模块不再自持 {@code order.cutoff-time} 之类的配置。
 */
public interface OrderService
{
	/**
	 * 查询订餐时间窗口（M2-01）
	 *
	 * @param date 就餐日期，为空取当天
	 * @return 时间窗口视图，含截止时间、配餐开始时间与当前是否可下单
	 */
	ServiceWindowVO getWindow(LocalDate date);

	/**
	 * 提交订单（M2-02）
	 * <p>
	 * 校验链：① 当前登录用户 → ② 时间窗口 → ③ `(employeeId, orderDate)` 唯一 →
	 * ④ 取当日菜单并校验菜品在菜单内 → ⑤ 写明细快照 → ⑥ 计算总价 → ⑦ 落库 → ⑧ 发事件。
	 *
	 * @param cmd 下单请求，身份取自登录上下文
	 * @return 创建后的订单视图
	 */
	OrderVO submit(OrderSubmitCmd cmd);

	/**
	 * 修改订单（M2-03）
	 *
	 * @param orderId 订单ID
	 * @param cmd     改单请求
	 */
	void modify(Long orderId, OrderModifyCmd cmd);

	/**
	 * 取消订单（M2-04）：本人 + 截止时间前
	 *
	 * @param orderId 订单ID
	 * @param reason  取消原因
	 */
	void cancel(Long orderId, String reason);

	/**
	 * 作废违规订单（M2-05）：经理权限，逻辑删除并留痕
	 *
	 * @param orderId 订单ID
	 * @param reason  作废原因，必填
	 * @return 审计ID
	 */
	String invalidate(Long orderId, String reason);

	/**
	 * 统计某员工某日的有效订单数（供“一人一天一单”判定与前端提示）
	 *
	 * @param employeeId 员工ID
	 * @param date       就餐日期
	 * @return 有效订单数
	 */
	int countDailyOrders(Long employeeId, LocalDate date);

	/**
	 * 分页查询订单（M2-07）
	 * <p>
	 * 员工角色由服务层强制注入 `employeeId = 当前用户`；经理/财务可自由筛选，越权抛 40300。
	 *
	 * @param query 查询条件
	 * @return 分页订单
	 */
	PageResult<OrderVO> page(OrderQuery query);

	/**
	 * 查询个人历史订单（M2-08）
	 *
	 * @param query 历史查询条件（含日期区间与状态）
	 * @return 分页订单
	 */
	PageResult<OrderVO> pageHistory(OrderHistoryQuery query);
}
