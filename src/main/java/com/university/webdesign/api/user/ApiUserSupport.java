package com.university.webdesign.api.user;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;

/**
 * 用户与报表中心 REST 控制器的公共基类。
 * <p>
 * 只放**协议与鉴权入口**相关的极小工具：当前登录用户读取与角色校验，
 * 不放任何业务判断（业务规则一律在 service 层）。
 * <p>
 * //todo 确认：Spring Security 目前仍是开发期全放行配置，因此角色校验暂时由
 * api 层显式调用本基类的 {@link #requireAnyRole(String, String...)} 完成；
 * 待 {@code config.SecurityConfig} 接入按角色授权（或新增 {@code AuthInterceptor}）后，
 * 这里的显式校验可以保留为纵深防御，也可以删除。
 */
abstract class ApiUserSupport
{
	/**
	 * 取当前登录用户，未登录抛 40100
	 *
	 * @return 登录用户上下文
	 */
	protected UserContext currentUser() {
		return UserContextHolder.require();
	}

	/**
	 * 校验当前登录用户拥有指定角色之一，否则抛 40300
	 *
	 * @param action    动作名称，用于中文提示
	 * @param roleCodes 允许的角色编码
	 * @return 登录用户上下文
	 */
	protected UserContext requireAnyRole(String action, String... roleCodes) {
		UserContext context = UserContextHolder.require();
		if (context.hasAnyRole(roleCodes)) {
			return context;
		}
		throw new BusinessException(ErrorCode.FORBIDDEN, "无权执行该操作：" + action);
	}
}
