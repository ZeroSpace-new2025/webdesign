package com.university.webdesign.service.operation.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 配送任务视图（`delivery_task`）。
 * <p>
 * 对应《对外方法表》M3-08 / M3-09：含菜名、分量、工位、电话。
 */
@Data
public class DeliveryTaskVO
{
	/**
	 * 任务ID
	 */
	private Long taskId;

	/**
	 * 任务编号，形如 `DT-20260101-1`
	 */
	private String taskNo;

	/**
	 * 就餐日期
	 */
	private java.time.LocalDate deliveryDate;

	/**
	 * 员工ID
	 */
	private Long employeeId;

	/**
	 * 员工姓名（由 `UserService.listByIds` 补齐）
	 */
	private String employeeName;

	/**
	 * 部门ID
	 */
	private Long deptId;

	/**
	 * 部门名称
	 */
	private String deptName;

	/**
	 * 工位（配送单核心字段）
	 */
	private String workstation;

	/**
	 * 联系电话
	 */
	private String phone;

	/**
	 * 状态编码
	 */
	private String status;

	/**
	 * 状态中文文案
	 */
	private String statusText;

	/**
	 * 打印次数（审计用）
	 */
	private int printCount;

	/**
	 * 最近打印时间（`yyyy-MM-dd HH:mm:ss`，可为空）
	 */
	private String printedAt;

	/**
	 * 签收人
	 */
	private String receiver;

	/**
	 * 备注
	 */
	private String remark;

	/**
	 * 送达时间（`yyyy-MM-dd HH:mm:ss`，可为空）
	 */
	private String deliveredAt;

	/**
	 * 明细（菜名、分量、单位）
	 */
	private List<DeliveryItemVO> items = new ArrayList<>();
}
