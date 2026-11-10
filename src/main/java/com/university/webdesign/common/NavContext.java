package com.university.webdesign.common;

import java.util.List;
import java.util.Map;

/**
 * 顶部导航栏的权限上下文（页面模型属性名 + 填充逻辑）。
 * <p>
 * 导航栏片段 `fragments/layout :: navbar` 只渲染当前账号有权限的入口，因此每个渲染页面视图的
 * 请求都要在模型里带上两样东西：
 * <ol>
 *     <li>{@link #PERM_CODES}：当前账号的权限点编码（与 JWT 载荷、{@code @RequiresPerm} 同一口径）；</li>
 *     <li>{@link #ROLE_CODES}：当前账号的角色名称（个别入口按预置角色放行，如财务管理看报表）。</li>
 * </ol>
 * 未登录时两个集合都是空的，导航栏只剩品牌与首页链接。
 * 正常页面由 `config.NavPermissionInterceptor` 在视图渲染前统一填充；错误页
 * （`templates/error.html`）不是控制器返回的视图，由 {@code GlobalExceptionHandler} 调用
 * {@link #fill(Map)} 补同一份数据，避免出错后导航栏变成空的。
 */
public final class NavContext
{
	/**
	 * 模型属性：当前登录用户的权限点编码集合
	 */
	public static final String PERM_CODES = "navPermCodes";

	/**
	 * 模型属性：当前登录用户的角色名称集合
	 */
	public static final String ROLE_CODES = "navRoleCodes";

	private NavContext() {
	}

	/**
	 * 把当前登录用户的权限点与角色写入页面模型
	 * <p>
	 * 未登录时写入空集合（而不是不写），这样模板里的 `#lists.contains` 判断不需要额外判空。
	 * 入参用 {@link Map} 而不是 {@code Model}：控制器返回的 {@code ModelAndView} 暴露的是
	 * {@code ModelMap}（视图模型本身就是一张表），两类调用方共用同一份填充逻辑。
	 *
	 * @param model 页面模型
	 */
	public static void fill(Map<String, Object> model) {
		if (model == null) {
			return;
		}
		UserContext context = UserContextHolder.get();
		List<String> permCodes = context == null || context.permCodes() == null
				? List.of() : context.permCodes();
		List<String> roleCodes = context == null || context.roles() == null
				? List.of() : context.roles();
		model.put(PERM_CODES, permCodes);
		model.put(ROLE_CODES, roleCodes);
	}
}
