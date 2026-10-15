package com.university.webdesign.service.operation;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.service.operation.dto.DeliveryQuery;
import com.university.webdesign.service.operation.dto.DeliveryTaskVO;
import com.university.webdesign.service.operation.dto.PrintBatchVO;
import com.university.webdesign.service.operation.dto.TaskStatus;
import com.university.webdesign.service.order.dto.ExportFormat;
import org.springframework.core.io.Resource;

import java.time.LocalDate;
import java.util.List;

/**
 * 运营与履约系统——配送服务。
 * <p>
 * 对应《对外方法表》4.2 的 `DeliveryService`：到达“配餐开始时间”（默认 11:30）后开放，
 * 按员工/工位维度拆分订单生成配送单，包含菜名、分量、工位、电话。
 * <p>
 * 取数走 {@code OrderQueryService.listValidByDate(date)}，员工信息走 {@code UserService.listByIds}，
 * 时间窗口走 {@code ServiceWindowService.get(date, scope, deptId)}。
 */
public interface DeliveryService
{
	/**
	 * 批量生成配送任务（M3-07）
	 * <p>
	 * 先校验已到配餐开始时间（未到抛 42201），再按员工维度拆分；已存在的任务跳过不重复派单。
	 *
	 * @param date       配送日期，为空取当天
	 * @param deptId     限定部门，可为空
	 * @param workstation 限定工位，可为空
	 * @return 本次生成的任务ID列表
	 */
	List<Long> generateTasks(LocalDate date, Long deptId, String workstation);

	/**
	 * 分页查询配送任务（M3-08）
	 *
	 * @param query 查询条件
	 * @return 分页结果
	 */
	PageResult<DeliveryTaskVO> page(DeliveryQuery query);

	/**
	 * 查询配送任务详情（M3-09）
	 *
	 * @param taskId 任务ID
	 * @return 任务视图（含明细），不存在抛 40400
	 */
	DeliveryTaskVO getDetail(Long taskId);

	/**
	 * 批量打印配送单（M3-10）
	 *
	 * @param taskIds    任务ID列表
	 * @param templateId 打印模板标识，可为空取默认模板
	 * @return 打印批次与数量
	 */
	PrintBatchVO batchPrint(List<Long> taskIds, String templateId);

	/**
	 * 更新配送状态（M3-11）
	 * <p>
	 * 状态机校验：`PENDING → DELIVERING → DELIVERED / EXCEPTION`，非法流转抛 40902。
	 *
	 * @param taskId   任务ID
	 * @param status   目标状态
	 * @param receiver 签收人
	 * @param remark   备注
	 */
	void updateStatus(Long taskId, TaskStatus status, String receiver, String remark);

	/**
	 * 导出配送台账（M3-12）
	 *
	 * @param date   配送日期，为空取当天
	 * @param deptId 限定部门，可为空
	 * @param fmt    导出格式
	 * @return 文件资源
	 */
	Resource export(LocalDate date, Long deptId, ExportFormat fmt);
}
