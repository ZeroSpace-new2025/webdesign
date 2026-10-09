package com.university.webdesign.web;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.config.TemporaryAdminAccount;
import com.university.webdesign.service.user.AuthService;
import com.university.webdesign.service.user.UserService;
import com.university.webdesign.service.user.dto.CurrentUserVO;
import com.university.webdesign.service.user.dto.UserVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 页面入口控制器（登录页 / 总门户 / 控制台）。
 * <p>
 * 与重构前的差异：登录态取自 {@code AuthInterceptor} 写入的 {@link UserContextHolder}
 * （JWT 解析结果），不再使用 Servlet Session 里自报的 `LOGIN_USER_ID`——那样的身份可以被
 * 客户端随意构造，角色校验形同虚设。
 * <p>
 * 登录成功后的落地页是**总门户** {@code /portal}：按当前账号的权限点列出各功能入口，
 * 再由此进入点餐、菜品/菜单管理、控制台等具体页面。
 */
@Controller
public class PageController
{
	private final UserService userService;
	private final AuthService authService;
	private final TemporaryAdminAccount temporaryAdmin;

	public PageController(UserService userService, AuthService authService,
			TemporaryAdminAccount temporaryAdmin) {
		this.userService = userService;
		this.authService = authService;
		this.temporaryAdmin = temporaryAdmin;
	}

	/**
	 * 已登录进入总门户，未登录跳转登录页
	 *
	 * @return 重定向
	 */
	@GetMapping("/")
	public String index() {
		return UserContextHolder.get() == null ? "redirect:/login" : "redirect:/portal";
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
	 * 总门户：登录后的落地页，按权限点展示各功能入口
	 * <p>
	 * 只依赖 token 里的角色与权限点（{@link UserContext}），因此临时硬编码管理员
	 * （不落库）也能正常打开；具体页面的数据仍由各页自己的控制器加载。
	 *
	 * @param model 视图模型
	 * @return 视图名或重定向
	 */
	@GetMapping("/portal")
	public String portal(Model model) {
		UserContext context = UserContextHolder.get();
		if (context == null || context.userId() == null) {
			return "redirect:/login";
		}
		List<String> permCodes = context.permCodes() == null ? List.of() : context.permCodes();
		model.addAttribute("permCodes", permCodes);
		model.addAttribute("roleNames", context.roles() == null ? List.of() : context.roles());
		model.addAttribute("employeeNo", context.employeeNo());
		model.addAttribute("userName", context.name());
		model.addAttribute("permissionCount", permCodes.size());
		// 临时硬编码管理员：页面上明确提示，避免误当成正式账号
		model.addAttribute("temporaryAdmin", temporaryAdmin.isAdmin(context.userId()));
		return "portal";
	}

	/**
	 * 控制台页面：注入当前登录用户的角色与权限点概览，业务数据由前端 JS 调 /api/v1/** 加载
	 * <p>
	 * 临时硬编码管理员不落库，{@code UserService.getById} 查不到时不报错，
	 * 改为用 token 里的身份信息兜底，保证控制台页面仍可打开。
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
		CurrentUserVO current = authService.currentUser(context.userId());
		model.addAttribute("currentUser", resolveCurrentUser(context, current));
		model.addAttribute("permCodes", current.getPermCodes());
		model.addAttribute("temporaryAdmin", temporaryAdmin.isAdmin(context.userId()));
		return "console";
	}

	/**
	 * 取控制台页面用的用户视图：优先查库，查不到（临时管理员）时用 token 信息兜底
	 *
	 * @param context token 里的登录上下文
	 * @param current 当前用户视图
	 * @return 用户视图
	 */
	private UserVO resolveCurrentUser(UserContext context, CurrentUserVO current) {
		try {
			return userService.getById(context.userId());
		} catch (BusinessException exception) {
			UserVO vo = new UserVO();
			vo.setUserId(context.userId());
			vo.setEmployeeNo(context.employeeNo());
			vo.setName(context.name());
			vo.setDeptId(context.deptId());
			vo.setWorkstation(context.workstation());
			vo.setPhone(context.phone());
			vo.setStatus(current.getStatus());
			vo.setStatusText(current.getStatusText());
			vo.setRoleCodes(current.getRoles());
			return vo;
		}
	}
}
