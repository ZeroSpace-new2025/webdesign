package com.university.webdesign.service.report;

import com.university.webdesign.service.report.dto.DeptConsumptionVO;
import com.university.webdesign.service.report.dto.EmployeeConsumptionVO;

import java.time.YearMonth;
import java.util.List;

/**
 * 员工消费审计服务。
 * <p>
 * 对应《对外方法表》5.2 的 {@code ConsumptionAuditService}：员工月度订单汇总与消费明细、
 * 部门维度消费汇总。
 * <p>
 * 权限：财务管理（{@code RoleCodes.FINANCE}）或餐厅经理（{@code RoleCodes.MANAGER}），
 * 由实现类校验，越权抛 40300。
 * <p>
 * 取数边界：明细一律经
 * {@link com.university.webdesign.service.order.OrderQueryService} 读取快照，
 * **不得**直接访问订单表。
 */
public interface ConsumptionAuditService
{
	/**
	 * 员工消费审计（M4-25）
	 * <p>
	 * 返回该员工当月有效订单数与消费总额；{@code withDetails=true} 时附带逐单明细，
	 * 明细条数上限 100，超出部分截断（前端翻页用订单模块的历史接口）。
	 *
	 * @param employeeId  员工ID
	 * @param month       月份
	 * @param withDetails 是否返回逐单明细
	 * @return 员工月度消费汇总
	 */
	EmployeeConsumptionVO auditEmployee(Long employeeId, YearMonth month, boolean withDetails);

	/**
	 * 部门消费汇总（M4-26）
	 * <p>
	 * 按员工所属部门分组汇总；{@code deptIds} 为空表示全部部门。
	 *
	 * @param month   月份
	 * @param deptIds 部门ID集合，可为空
	 * @return 部门维度汇总列表
	 */
	List<DeptConsumptionVO> sumByDept(YearMonth month, List<Long> deptIds);
}
