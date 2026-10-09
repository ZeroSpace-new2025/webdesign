package com.university.webdesign.service.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单视图对象（含下单时刻明细快照）。
 * <p>
 * 对应《对外方法表》M2-06 / M2-07 的 `OrderVO`，是订单模块对外的统一读模型：
 * 报表、消费审计、总括订单聚合、配送单生成都按本结构取数。
 */
@Data
public class OrderVO
{
	/**
	 * 订单ID
	 */
	private Long orderId;

	/**
	 * 订单号（对外展示与打印）
	 */
	private String orderNo;

	/**
	 * 员工ID
	 */
	private Long employeeId;

	/**
	 * 员工姓名（由 `UserService.listByIds` 补齐，可能为空）
	 */
	private String employeeName;

	/**
	 * 部门ID
	 */
	private Long deptId;

	/**
	 * 部门名称（补齐字段，可能为空）
	 */
	private String deptName;

	/**
	 * 就餐日期
	 */
	private java.time.LocalDate orderDate;

	/**
	 * 订单状态编码
	 */
	private String status;

	/**
	 * 订单状态中文文案
	 */
	private String statusText;

	/**
	 * 总金额
	 */
	private BigDecimal totalAmount;

	/**
	 * 备注
	 */
	private String remark;

	/**
	 * 明细快照
	 */
	private List<OrderDetailVO> items = new ArrayList<>();

	/**
	 * 下单时间（`yyyy-MM-dd HH:mm:ss`）
	 */
	private String createdAt;

	/**
	 * 最后修改时间（`yyyy-MM-dd HH:mm:ss`）
	 */
	private String updatedAt;

	/**
	 * 作废人ID（仅 `INVALID` 状态有值）
	 */
	private Long invalidatedBy;

	/**
	 * 作废原因（仅 `INVALID` 状态有值）
	 */
	private String invalidateReason;

	/**
	 * 乐观锁版本号
	 */
	private Integer version;
}
