package com.university.webdesign.api.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新账号状态请求（M4-12）。
 * <p>
 * 取值 {@code ACTIVE} / {@code DISABLED} / {@code LOCKED}；非法取值由 service 层抛 40001。
 */
@Data
public class UserStatusRequest
{
	/**
	 * 目标状态编码
	 */
	@NotBlank(message = "账号状态不能为空")
	private String status;
}
