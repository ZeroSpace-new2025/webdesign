package com.university.webdesign.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 兼容入口：历史上根路径跳转到菜品管理，现在统一由 {@code web.PageController} 处理
 * （已登录进控制台、未登录进登录页）。
 * <p>
 * 保留本类仅为兼容直接访问 {@code /home} 的旧书签，不再映射 {@code "/"}，
 * 避免与 {@code PageController.index} 产生重复映射导致上下文启动失败。
 */
@Controller
public class HomeController {

	/**
	 * 旧书签入口
	 *
	 * @return 重定向到根路径
	 */
	@GetMapping("/home")
	public String home() {
		return "redirect:/";
	}
}
