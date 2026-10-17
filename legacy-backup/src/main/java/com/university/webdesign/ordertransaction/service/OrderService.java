package com.university.webdesign.ordertransaction.service;

import com.university.webdesign.ordertransaction.api.OrderCreateData;
import com.university.webdesign.ordertransaction.api.OrderDTO;
import com.university.webdesign.ordertransaction.api.OrderQueryData;
import com.university.webdesign.ordertransaction.api.OrderUpdateData;
import com.university.webdesign.ordertransaction.api.PersonalConsumptionDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 订单与交易核心服务
 * <p>
 * 业务规则集中在本接口的实现中：
 * <ul>
 *     <li>时间窗口：当日订单必须在“订餐截止时间”前提交，截止后禁止新增/修改；</li>
 *     <li>一人一天一单：同一员工同一天只能有一张有效订单；</li>
 *     <li>数据快照：明细冗余保存下单时的菜名、分类、单价，菜谱变动不影响历史订单。</li>
 * </ul>
 */
@Component
public interface OrderService
{
	/**
	 * 获取今天的订单列表（经理视角）
	 *
	 * @return 今天所有有效订单
	 */
	List<OrderDTO> getTodayOrders();
	
	/**
	 * 按条件查询订单
	 *
	 * @param queryData 查询条件，为 null 时返回今日全部有效订单
	 * @return 订单列表
	 */
	List<OrderDTO> query(OrderQueryData queryData);
	
	/**
	 * 按订单ID查询订单
	 *
	 * @param orderId 订单ID
	 * @return 订单，不存在时返回 null
	 */
	OrderDTO getOrder(Long orderId);
	
	/**
	 * 创建订单
	 *
	 * @param createData 下单请求（员工ID + 菜品ID与数量）
	 * @return 创建后的订单
	 */
	OrderDTO createOrder(OrderCreateData createData);
	
	/**
	 * 创建订单（兼容旧签名）
	 *
	 * @param userId 员工ID
	 * @return 创建后的订单
	 */
	OrderDTO createOrder(Long userId);
	
	/**
	 * 修改订单（员工在截止时间前修改自己订单的菜品与数量）
	 *
	 * @param updateData 改单请求
	 * @return 修改后的订单
	 */
	OrderDTO updateOrder(OrderUpdateData updateData);
	
	/**
	 * 修改订单（兼容旧签名，仅按传入的明细整体替换）
	 *
	 * @param orderDTO 订单DTO，需包含 orderId 与 items
	 * @return 修改后的订单
	 */
	OrderDTO updateOrder(OrderDTO orderDTO);
	
	/**
	 * 订单支付，将订单从“未支付”推进为“已支付”
	 *
	 * @param orderId    订单ID
	 * @param operatorId 操作用户ID
	 * @return 支付后的订单
	 */
	OrderDTO payOrder(Long orderId, Long operatorId);
	
	/**
	 * 取消订单（员工在截止时间前取消自己的订单）
	 *
	 * @param orderId    订单ID
	 * @param operatorId 操作用户ID
	 * @return 取消后的订单
	 */
	OrderDTO cancelOrder(Long orderId, Long operatorId);
	
	/**
	 * 删除订单（经理删除违规订单，以取消状态留痕，不做物理删除）
	 *
	 * @param orderId    订单ID
	 * @param operatorId 操作经理ID；为 null 时视为可信内部调用，不做角色校验
	 * @return 删除（取消）后的订单
	 */
	OrderDTO deleteOrder(Long orderId, Long operatorId);
	
	/**
	 * 删除订单（兼容旧签名）
	 *
	 * @param orderId 订单ID
	 * @return 删除（取消）后的订单
	 */
	OrderDTO deleteOrder(Long orderId);
	
	/**
	 * 个人历史订单查询
	 * <p>
	 * 只能查自己的订单；经理与财务可查任意员工（消费审计场景）。
	 *
	 * @param userId     要查询的员工ID
	 * @param operatorId 发起查询的用户ID；为 null 时视为可信内部调用
	 * @param start      起始日期（含），可为 null 表示不限
	 * @param end        结束日期（含），可为 null 表示不限
	 * @return 该员工的历史订单，按下单时间升序
	 */
	List<OrderDTO> getHistoryOrders(Long userId, Long operatorId, LocalDate start, LocalDate end);
	
	/**
	 * 个人历史订单查询（兼容旧签名，不校验操作人）
	 *
	 * @param userId 员工ID
	 * @param start  起始日期（含），可为 null 表示不限
	 * @param end    结束日期（含），可为 null 表示不限
	 * @return 该员工的历史订单，按下单时间升序
	 */
	List<OrderDTO> getHistoryOrders(Long userId, LocalDate start, LocalDate end);
	
	/**
	 * 个人月度消费统计
	 * <p>
	 * 只能查自己；经理与财务可查任意员工（消费审计场景）。
	 *
	 * @param userId     要查询的员工ID
	 * @param operatorId 发起查询的用户ID；为 null 时视为可信内部调用
	 * @param year       年份
	 * @param month      月份（1-12）
	 * @return 月度消费统计（含明细与按菜品汇总）
	 */
	PersonalConsumptionDTO getMonthlyConsumption(Long userId, Long operatorId, int year, int month);
	
	/**
	 * 个人月度消费统计（兼容旧签名，不校验操作人）
	 *
	 * @param userId 员工ID
	 * @param year   年份
	 * @param month  月份（1-12）
	 * @return 月度消费统计（含明细与按菜品汇总）
	 */
	PersonalConsumptionDTO getMonthlyConsumption(Long userId, int year, int month);
	
	/**
	 * 查询指定日期某个员工的有效订单
	 * <p>
	 * 供履约模块判断“该员工当日是否已点餐”使用；当天订单只应存在一张。
	 *
	 * @param userId   员工ID
	 * @param workDate 就餐日期
	 * @return 有效订单，不存在时返回 null
	 */
	OrderDTO findActiveOrder(Long userId, LocalDate workDate);
	
	/**
	 * 校验当前时刻是否仍在“订餐截止时间”之前
	 *
	 * @param workDate 就餐日期
	 * @return 未截止返回 true
	 */
	boolean isBeforeCutoff(LocalDate workDate);
}
