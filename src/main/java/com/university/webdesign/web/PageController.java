package com.university.webdesign.web;

import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.service.user.AuthService;
import com.university.webdesign.service.user.UserService;
import com.university.webdesign.service.user.dto.UserVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面入口控制器（登录页 / 控制台）。
 * <p>
 * 与重构前的差异：登录态取自 {@code AuthInterceptor} 写入的 {@link UserContextHolder}
 * （JWT 解析结果），不再使用 Servlet Session 里自报的 `LOGIN_USER_ID`——那样的身份可以被
 * 客户端随意构造，角色校验形同虚设。
 */
@Controller
public class PageController
{
	private final UserService userService;
	private final AuthService authService;

	public PageController(UserService userService, AuthService authService) {
		this.userService = userService;
		this.authService = authService;
	}

	/**
	 * 已登录进入控制台，未登录跳转登录页
	 *
	 * @return 重定向
	 */
	@GetMapping("/")
	public String index() {
		return UserContextHolder.get() == null ? "redirect:/login" : "redirect:/console";
	}

	/**
	 * 登录页面不经过数据库，直接由 Thymeleaf 渲染
	 *
	 * @return 视图名
	 */
	@GetMapping("/login")
	public String login() {
		return "login";
	}

	/**
	 * 控制台页面：注入当前登录用户的角色与权限点概览，业务数据由前端 JS 调 /api/v1/** 加载
	 *
	 * @param model 视图模型
	 * @return 视图名或重定向
	 */
	@GetMapping("/console")
	public String console(Model model) {
		UserContext context = UserContextHolder.get();
		if (context == null || context.userId() == null) {
			return "redirect:/login";
		}
		UserVO currentUser = userService.getById(context.userId());
		model.addAttribute("currentUser", currentUser);
		model.addAttribute("permCodes", authService.currentUser(context.userId()).getPermCodes());
		return "console";
	}
}
