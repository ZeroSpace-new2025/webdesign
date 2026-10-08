package com.university.webdesign.api.menu;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.PermCodes;
import com.university.webdesign.common.Result;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.config.RequiresPerm;
import com.university.webdesign.service.menu.MenuService;
import com.university.webdesign.service.menu.dto.MenuCreateCmd;
import com.university.webdesign.service.menu.dto.MenuQuery;
import com.university.webdesign.service.menu.dto.MenuUpdateCmd;
import com.university.webdesign.service.menu.dto.MenuVO;
import com.university.webdesign.service.menu.dto.PublishResultVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 菜单管理 REST 接口（M1-08 ~ M1-13），基础路径 {@code /api/v1/menu/menus}。
 * <p>
 * {@code GET /menus/current} 是员工点餐页取当日菜单（含冻结快照价）的入口，
 * 底层与 M2 下单共用 {@code MenuService.getCurrent(date)}，保证“页面看到的价格 = 下单取价”。
 * <p>
 * 分层约定：本类只做 HTTP 协议转换与 {@code Result} 组装，不写任何业务判断。
 * <p>
 * 写操作按《对外方法表》M1 的角色要求用 {@link RequiresPerm} 声明权限点
 * （{@code menu:menu:manage}，或餐厅经理角色），由 {@code AuthInterceptor} 统一校验；
 * 查询接口对所有已登录角色开放。
 */
@RestController
@RequestMapping("/api/v1/menu/menus")
public class ApiMenuController
{
	private final MenuService menuService;

	public ApiMenuController(MenuService menuService) {
		this.menuService = menuService;
	}

	/**
	 * 创建菜单草稿（M1-08）
	 *
	 * @param cmd 创建请求（name、effectiveDate、items[{recipeId, quantity, menuPrice}]）
	 * @return 菜单ID + 版本号 + 状态
	 */
	@PostMapping
	@RequiresPerm(value = PermCodes.MENU_MENU_MANAGE, roles = RoleCodes.MANAGER)
	public Result<MenuCreateResultVO> createDraft(@Valid @RequestBody MenuCreateCmd cmd) {
		Long menuId = menuService.createDraft(cmd);
		MenuVO menu = findMenu(menuId);
		MenuCreateResultVO result = new MenuCreateResultVO();
		result.setMenuId(menuId);
		if (menu != null) {
			result.setVersion(menu.getVersion());
			result.setStatus(menu.getStatus());
		}
		return Result.success(result);
	}

	/**
	 * 更新菜单（M1-09）
	 *
	 * @param menuId 菜单ID
	 * @param cmd    更新请求（仅 DRAFT 可改，含 version 乐观锁）
	 * @return 更新后的新版本号
	 */
	@PutMapping("/{menuId}")
	@RequiresPerm(value = PermCodes.MENU_MENU_MANAGE, roles = RoleCodes.MANAGER)
	public Result<Integer> update(@PathVariable("menuId") Long menuId, @Valid @RequestBody MenuUpdateCmd cmd) {
		return Result.success(menuService.update(menuId, cmd));
	}

	/**
	 * 发布菜单（M1-10）
	 *
	 * @param menuId 菜单ID
	 * @return 发布结果（status、publishedAt、snapshotCount、version）
	 */
	@PostMapping("/{menuId}/publish")
	@RequiresPerm(value = PermCodes.MENU_MENU_MANAGE, roles = RoleCodes.MANAGER)
	public Result<PublishResultVO> publish(@PathVariable("menuId") Long menuId) {
		return Result.success(menuService.publish(menuId));
	}

	/**
	 * 下架菜单（M1-11）
	 *
	 * @param menuId 菜单ID
	 * @param reason 下架原因，可选
	 * @return 下架后的菜单视图（含 status 与 offlineAt）
	 */
	@PostMapping("/{menuId}/unpublish")
	@RequiresPerm(value = PermCodes.MENU_MENU_MANAGE, roles = RoleCodes.MANAGER)
	public Result<MenuVO> unpublish(
			@PathVariable("menuId") Long menuId,
			@RequestParam(value = "reason", required = false) String reason) {
		menuService.unpublish(menuId, reason);
		return Result.success(findMenu(menuId));
	}

	/**
	 * 查询当前有效菜单（M1-12）
	 *
	 * @param date 就餐日期，格式 {@code yyyy-MM-dd}，缺省取当天
	 * @return 当日已发布菜单（含快照价与分类分组），无可用菜单时 data 为 null
	 */
	@GetMapping("/current")
	public Result<MenuVO> current(
			@RequestParam(value = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return Result.success(menuService.getCurrent(date));
	}

	/**
	 * 查询历史菜单与版本（M1-13）
	 *
	 * @param query 查询条件（dateFrom、dateTo、status、menuId、withItems + 分页）
	 * @return 分页菜单；{@code withItems=true} 时带明细供复用
	 */
	@GetMapping
	public Result<PageResult<MenuVO>> page(@ModelAttribute MenuQuery query) {
		return Result.success(menuService.page(query));
	}

	/**
	 * 按ID取单份菜单（不含明细），用于组装“操作后立即回显”的响应
	 */
	private MenuVO findMenu(Long menuId) {
		MenuQuery query = new MenuQuery();
		query.setMenuId(menuId);
		query.setWithItems(Boolean.FALSE);
		PageResult<MenuVO> page = menuService.page(query);
		return page == null || page.getList() == null || page.getList().isEmpty() ? null : page.getList().get(0);
	}
}
