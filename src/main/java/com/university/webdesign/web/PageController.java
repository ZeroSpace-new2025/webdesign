package com.university.webdesign.web;

import com.university.webdesign.common.AuthSession;
import com.university.webdesign.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Thymeleaf页面入口。
 */
@Controller
public class PageController
{
	private final UserService userService;

	public PageController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * 已登录进入控制台，未登录跳转登录页。
	 */
	@GetMapping("/")
	public String index(HttpSession session) {
		return AuthSession.getUserId(session) == null ? "redirect:/login" : "redirect:/console";
	}

	/**
	 * 登录页面不经过数据库，直接由Thymeleaf渲染。
	 */
	@GetMapping("/login")
	public String login() {
		return "login";
	}

	/**
	 * 控制台页面依赖当前会话用户和前端JS加载业务数据。
	 */
	@GetMapping("/console")
	public String console(HttpSession session, Model model) {
		Long userId = AuthSession.getUserId(session);
		if (userId == null) {
			return "redirect:/login";
		}
		model.addAttribute("currentUser", userService.getUserInfo(userId));
		return "console";
	}
}
