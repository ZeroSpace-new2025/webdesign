package com.university.webdesign.api.operation;

import com.university.webdesign.common.PermCodes;
import com.university.webdesign.common.Result;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.config.RequiresPerm;
import com.university.webdesign.service.operation.ServiceWindowService;
import com.university.webdesign.service.operation.dto.ServiceWindowCmd;
import com.university.webdesign.service.operation.dto.ServiceWindowVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 服务周期（时间窗口）配置 REST 控制器。
 * <p>
 * 对应《对外方法表》M3 的 `ApiConfigController`（基础路径 `/api/v1/operation`）：
 * 新增配置（M3-13）、修改配置（M3-14）、查询配置（M3-15）。
 * 其中查询接口既是页面入口，也是 M2 下单校验所依赖的
 * {@code ServiceWindowService.get(date, scope, deptId)} 的 HTTP 形态。
 */
@RestController
@RequestMapping("/api/v1/operation")
public class ApiConfigController
{
	private final ServiceWindowService serviceWindowService;

	public ApiConfigController(ServiceWindowService serviceWindowService) {
		this.serviceWindowService = serviceWindowService;
	}

	/**
	 * M3-13 新增时间窗口配置
	 *
	 * @param request 配置入参
	 * @return 配置ID
	 */
	@PostMapping("/configs/service-window")
	@RequiresPerm(value = PermCodes.OPERATION_WINDOW_MANAGE,
			roles = {RoleCodes.MANAGER, RoleCodes.KITCHEN_SUPERVISOR})
	public Result<Long> create(@Valid @RequestBody ServiceWindowCreateRequest request) {
		return Result.success(serviceWindowService.create(toCmd(request)));
	}

	/**
	 * M3-14 修改时间窗口配置（改后立即生效）
	 *
	 * @param configId 配置ID
	 * @param request  配置入参
	 * @return 更新后的配置
	 */
	@PutMapping("/configs/service-window/{configId}")
	@RequiresPerm(value = PermCodes.OPERATION_WINDOW_MANAGE,
			roles = {RoleCodes.MANAGER, RoleCodes.KITCHEN_SUPERVISOR})
	public Result<ServiceWindowVO> update(@PathVariable("configId") Long configId,
			@Valid @RequestBody ServiceWindowUpdateRequest request) {
		serviceWindowService.update(configId, toCmd(request));
		return Result.success(serviceWindowService.get(LocalDate.now(), null, request.getDeptId()));
	}

	/**
	 * M3-15 查询时间窗口配置（无配置时回退默认 09:00 / 11:30）
	 *
	 * @param date   目标日期，为空取当天
	 * @param scope  作用域：GLOBAL / DEPT，为空按优先级自动选择
	 * @param deptId 部门ID，可为空
	 * @return 生效的时间窗口
	 */
	@GetMapping("/configs/service-window")
	public Result<ServiceWindowVO> get(
			@RequestParam(value = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "scope", required = false) String scope,
			@RequestParam(value = "deptId", required = false) Long deptId) {
		return Result.success(serviceWindowService.get(date, scope, deptId));
	}

	// ------------------------------------------------------------------ 内部方法

	private ServiceWindowCmd toCmd(ServiceWindowCreateRequest request) {
		ServiceWindowCmd cmd = new ServiceWindowCmd();
		cmd.setCutoffTime(request.getCutoffTime());
		cmd.setDeliveryStartTime(request.getDeliveryStartTime());
		cmd.setEffectiveFrom(request.getEffectiveFrom());
		cmd.setScope(request.getScope());
		cmd.setDeptId(request.getDeptId());
		return cmd;
	}

	private ServiceWindowCmd toCmd(ServiceWindowUpdateRequest request) {
		ServiceWindowCmd cmd = new ServiceWindowCmd();
		cmd.setCutoffTime(request.getCutoffTime());
		cmd.setDeliveryStartTime(request.getDeliveryStartTime());
		cmd.setEffectiveFrom(request.getEffectiveFrom());
		cmd.setScope(request.getScope());
		cmd.setDeptId(request.getDeptId());
		return cmd;
	}
}
