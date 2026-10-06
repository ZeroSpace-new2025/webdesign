package com.university.webdesign.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页路由：访问根路径时跳转到菜品管理。
 */
@Controller
public class HomeController {

	@GetMapping("/")
	public String home() {
		return "redirect:/recipes";
	}
}
