package com.university.webdesign.web;

import com.university.webdesign.user.api.AuthenticatedUser;
import com.university.webdesign.user.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;

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
	 * 根路径按登录角色落到对应首页，未登录统一去登录页。
	 */
	@GetMapping("/")
	public String index(Authentication authentication) {
		if (authentication == null
			|| !authentication.isAuthenticated()
			|| authentication instanceof AnonymousAuthenticationToken) {
			return "redirect:/login";
		}
		if (hasAnyRole(authentication, "MANAGER", "FINANCE")) {
			return "redirect:/console";
		}
		if (hasAnyRole(authentication, "KITCHEN_SUPERVISOR")) {
			return "redirect:/menus";
		}
		return "redirect:/order/create";
	}

	/**
	 * 登录页面不经过数据库，直接由Thymeleaf渲染。
	 */
	@GetMapping("/login")
	public String login() {
		return "login";
	}

	/**
	 * 控制台页面依赖当前认证用户和前端JS加载业务数据。
	 */
	@GetMapping("/console")
	public String console(Authentication authentication, Model model) {
		if (authentication == null
			|| !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
			return "redirect:/login";
		}
		model.addAttribute("currentUser", userService.getUserInfo(user.userId()));
		return "console";
	}

	private boolean hasAnyRole(Authentication authentication, String... roleCodes) {
		return Arrays.stream(roleCodes).anyMatch(roleCode -> authentication.getAuthorities().stream()
			.anyMatch(authority -> authority.getAuthority().equals("ROLE_" + roleCode)));
	}
}
