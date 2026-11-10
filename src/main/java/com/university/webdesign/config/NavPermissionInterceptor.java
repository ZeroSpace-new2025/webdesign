package com.university.webdesign.config;

import com.university.webdesign.common.NavContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 顶部导航权限拦截器。
 * <p>
 * 导航栏片段要按当前账号的权限点显示入口，但页面控制器的模型里只有业务数据、没有登录权限，
 * 这里在**视图渲染前**（{@link #postHandle}）统一把 `navPermCodes` / `navRoleCodes`
 * 注入模型，页面模板与各控制器都不用重复写这段逻辑。
 * <p>
 * 只处理“返回视图名”的页面请求：
 * <ul>
 *     <li>{@code redirect:} 跳转不注入，否则模型属性会被拼进跳转地址；</li>
 *     <li>{@code /api/**} 返回 JSON，没有视图，自然不会进入这里。</li>
 * </ul>
 * 登录态来自 {@link AuthInterceptor} 在 {@code preHandle} 写入的 ThreadLocal，
 * 到 {@code postHandle} 时仍然有效（清理发生在 {@code afterCompletion}）。
 */
@Component
public class NavPermissionInterceptor implements HandlerInterceptor
{
	/**
	 * 视图渲染前注入导航所需的权限与角色
	 *
	 * @param request      请求
	 * @param response     响应
	 * @param handler      处理器
	 * @param modelAndView 控制器返回的视图与模型；重定向、返回 body 的请求为 null
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler, ModelAndView modelAndView) {
		if (modelAndView == null) {
			return;
		}
		String viewName = modelAndView.getViewName();
		if (viewName == null || viewName.startsWith("redirect:")) {
			return;
		}
		NavContext.fill(modelAndView.getModelMap());
	}
}
