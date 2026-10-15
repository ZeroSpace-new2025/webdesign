package com.university.webdesign.service.operation;

import com.university.webdesign.service.operation.dto.ServiceWindowCmd;
import com.university.webdesign.service.operation.dto.ServiceWindowVO;

import java.time.LocalDate;

/**
 * 运营与履约系统——服务周期（时间窗口）配置服务。
 * <p>
 * 对应《对外方法表》4.2 的 `ServiceWindowService`。{@link #get(LocalDate, String, Long)}
 * 是**M2 下单校验时间窗口的唯一数据来源**：按 `GLOBAL` / `DEPT` 优先级取值，
 * 无配置时回退默认 09:00 / 11:30（见 {@code ServiceWindowProperties}）。
 */
public interface ServiceWindowService
{
	/**
	 * 新增时间窗口配置（M3-13）
	 *
	 * @param cmd 配置入参
	 * @return 配置ID
	 */
	Long create(ServiceWindowCmd cmd);

	/**
	 * 修改时间窗口配置（M3-14）：改后立即生效
	 *
	 * @param configId 配置ID
	 * @param cmd      配置入参
	 */
	void update(Long configId, ServiceWindowCmd cmd);

	/**
	 * 查询时间窗口（M3-15）：M2 下单校验与 M3 配送校验共用
	 *
	 * @param date  目标日期，为空取当天
	 * @param scope 作用域：GLOBAL / DEPT，为空按优先级自动选择
	 * @param deptId 部门ID，可为空
	 * @return 生效的时间窗口；数据库无配置时回退默认 09:00 / 11:30
	 */
	ServiceWindowVO get(LocalDate date, String scope, Long deptId);
}
