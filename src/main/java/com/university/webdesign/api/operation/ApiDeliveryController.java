package com.university.webdesign.api.operation;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.Result;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.config.RequiresPerm;
import com.university.webdesign.service.operation.DeliveryService;
import com.university.webdesign.service.operation.dto.DeliveryTaskVO;
import com.university.webdesign.service.operation.dto.PrintBatchVO;
import com.university.webdesign.service.operation.dto.TaskStatus;
import com.university.webdesign.service.order.dto.ExportFormat;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 配送管理 REST 控制器。
 * <p>
 * 对应《对外方法表》M3 的 `ApiDeliveryController`（基础路径 `/api/v1/operation`）：
 * 批量生成配送任务（M3-07）、分页查询（M3-08）、详情（M3-09）、批量打印（M3-10）、
 * 更新状态（M3-11）、导出台账（M3-12）。
 * <p>
 * 本层只做协议转换；配餐开始时间校验、状态机校验、打印次数累加全部在
 * {@link DeliveryService} 中；导出接口直接返回文件流，不包 {@code Result}。
 */
@RestController
@RequestMapping("/api/v1/operation")
public class ApiDeliveryController
{
	private final DeliveryService deliveryService;

	public ApiDeliveryController(DeliveryService deliveryService) {
		this.deliveryService = deliveryService;
	}

	/**
	 * M3-07 批量生成配送任务
	 *
	 * @param date       配送日期，为空取当天
	 * @param deptId     限定部门，可为空
	 * @param workstation 限定工位，可为空
	 * @return 本次生成的任务ID列表
	 */
	@PostMapping("/deliveries/generate")
	@RequiresPerm(value = PermissionEnum.OPERATION_DELIVERY_PRINT,
			roles = {RoleCodes.DELIVERY_STAFF, RoleCodes.KITCHEN_SUPERVISOR, RoleCodes.MANAGER})
	public Result<List<Long>> generate(
			@RequestParam(value = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "deptId", required = false) Long deptId,
			@RequestParam(value = "workstation", required = false) String workstation) {
		return Result.success(deliveryService.generateTasks(date, deptId, workstation));
	}

	/**
	 * M3-08 分页查询配送任务
	 *
	 * @param query 查询条件（日期、部门、工位、员工、状态、分页）
	 * @return 分页任务
	 */
	@GetMapping("/deliveries")
	@RequiresPerm(value = PermissionEnum.OPERATION_DELIVERY_PRINT,
			roles = {RoleCodes.DELIVERY_STAFF, RoleCodes.KITCHEN_SUPERVISOR, RoleCodes.MANAGER})
	public Result<PageResult<DeliveryTaskVO>> page(@ModelAttribute DeliveryPageRequest query) {
		return Result.success(deliveryService.page(query));
	}

	/**
	 * M3-09 查询配送任务详情
	 *
	 * @param taskId 任务ID
	 * @return 任务详情（含明细）
	 */
	@GetMapping("/deliveries/{taskId}")
	@RequiresPerm(value = PermissionEnum.OPERATION_DELIVERY_PRINT,
			roles = {RoleCodes.DELIVERY_STAFF, RoleCodes.KITCHEN_SUPERVISOR, RoleCodes.MANAGER})
	public Result<DeliveryTaskVO> detail(@PathVariable("taskId") Long taskId) {
		return Result.success(deliveryService.getDetail(taskId));
	}

	/**
	 * M3-10 批量打印配送单
	 *
	 * @param request 打印入参（任务ID列表、模板标识）
	 * @return 打印批次与数量
	 */
	@PostMapping("/deliveries/batch-print")
	@RequiresPerm(value = PermissionEnum.OPERATION_DELIVERY_PRINT,
			roles = {RoleCodes.DELIVERY_STAFF, RoleCodes.KITCHEN_SUPERVISOR, RoleCodes.MANAGER})
	public Result<PrintBatchVO> batchPrint(
			@RequestBody(required = false) BatchPrintRequest request,
			@RequestParam(value = "templateId", required = false) String templateId) {
		BatchPrintRequest effective = request == null ? new BatchPrintRequest() : request;
		String template = effective.getTemplateId() != null ? effective.getTemplateId() : templateId;
		return Result.success(deliveryService.batchPrint(effective.getTaskIds(), template));
	}

	/**
	 * M3-11 更新配送状态
	 *
	 * @param taskId  任务ID
	 * @param request 状态入参（status、receiver、remark）
	 * @return 更新后的任务详情
	 */
	@PutMapping("/deliveries/{taskId}/status")
	@RequiresPerm(value = PermissionEnum.OPERATION_DELIVERY_PRINT,
			roles = {RoleCodes.DELIVERY_STAFF, RoleCodes.KITCHEN_SUPERVISOR, RoleCodes.MANAGER})
	public Result<DeliveryTaskVO> updateStatus(@PathVariable("taskId") Long taskId,
			@RequestBody(required = false) DeliveryStatusRequest request) {
		DeliveryStatusRequest effective = request == null ? new DeliveryStatusRequest() : request;
		TaskStatus status = TaskStatus.parse(effective.getStatus());
		deliveryService.updateStatus(taskId, status, effective.getReceiver(), effective.getRemark());
		return Result.success(deliveryService.getDetail(taskId));
	}

	/**
	 * M3-12 导出配送台账
	 *
	 * @param date   配送日期，为空取当天
	 * @param deptId 限定部门，可为空
	 * @param format 导出格式：XLSX（默认）/ CSV
	 * @return 文件流
	 */
	@GetMapping("/deliveries/export")
	@RequiresPerm(value = PermissionEnum.OPERATION_DELIVERY_PRINT,
			roles = {RoleCodes.DELIVERY_STAFF, RoleCodes.KITCHEN_SUPERVISOR, RoleCodes.MANAGER})
	public ResponseEntity<byte[]> export(
			@RequestParam(value = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "deptId", required = false) Long deptId,
			@RequestParam(value = "format", required = false, defaultValue = "XLSX") String format) {
		ExportFormat exportFormat = ExportFormat.parse(format);
		Resource resource = deliveryService.export(date, deptId, exportFormat);
		boolean excel = exportFormat != ExportFormat.CSV;
		return OperationFileResponses.download(resource, excel,
				excel ? "delivery-ledger.xls" : "delivery-ledger.csv");
	}
}
