package com.university.webdesign.web.menu;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.domain.menu.MenuStatus;
import com.university.webdesign.domain.menu.RecipeStatus;
import com.university.webdesign.service.menu.MenuService;
import com.university.webdesign.service.menu.RecipeService;
import com.university.webdesign.service.menu.dto.MenuCreateCmd;
import com.university.webdesign.service.menu.dto.MenuCreateItem;
import com.university.webdesign.service.menu.dto.MenuQuery;
import com.university.webdesign.service.menu.dto.MenuUpdateCmd;
import com.university.webdesign.service.menu.dto.MenuUpdateItem;
import com.university.webdesign.service.menu.dto.MenuVO;
import com.university.webdesign.service.menu.dto.RecipeQuery;
import com.university.webdesign.service.menu.dto.RecipeVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单管理页面控制器（服务端渲染，走 {@code /menus/**}）。
 * <p>
 * 负责菜单列表、编排（挑菜品 + 菜单级调价）、发布 / 下架 / 复制为草稿。
 * 业务规则全部由 {@link MenuService} 判定：仅草稿可改（40902）、发布即冻结快照并锁定、
 * 已发布菜单不可编辑删除。页面只做视图组装与错误回显。
 * <p>
 * 发布时会真实写入 {@code menu_snapshot}，因此点餐页与下单链路取到的就是冻结价格。
 */
@Controller
@RequestMapping("/menus")
public class MenuPageController
{
	/**
	 * 菜单编排页一次展示的最大菜品数（菜品库很小，直接取分页上限）
	 */
	private static final int QUERY_PAGE_SIZE = 100;

	private final MenuService menuService;
	private final RecipeService recipeService;

	public MenuPageController(MenuService menuService, RecipeService recipeService) {
		this.menuService = menuService;
		this.recipeService = recipeService;
	}

	/**
	 * 菜单列表
	 *
	 * @param model 视图模型
	 * @return 视图名
	 */
	@GetMapping
	public String list(Model model) {
		MenuQuery query = new MenuQuery();
		query.setWithItems(Boolean.TRUE);
		query.setPageNum(1);
		query.setPageSize(QUERY_PAGE_SIZE);
		PageResult<MenuVO> page = menuService.page(query);
		model.addAttribute("menus", page.getList());
		return "menu/list";
	}

	/**
	 * 新建菜单表单
	 *
	 * @param model 视图模型
	 * @return 视图名
	 */
	@GetMapping("/new")
	public String newForm(Model model) {
		MenuForm form = new MenuForm();
		form.setEffectiveDate(LocalDate.now());
		form.setRows(buildRows(null));
		model.addAttribute("form", form);
		model.addAttribute("isEdit", false);
		return "menu/form";
	}

	/**
	 * 编辑菜单表单（仅草稿可编辑）
	 *
	 * @param id       菜单ID
	 * @param model    视图模型
	 * @param redirect 重定向属性
	 * @return 视图名或重定向
	 */
	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirect) {
		MenuVO menu = findMenu(id);
		if (menu == null) {
			redirect.addFlashAttribute("error", "菜单不存在：" + id);
			return "redirect:/menus";
		}
		if (menu.isLocked() || !MenuStatus.DRAFT.name().equals(menu.getStatus())) {
			redirect.addFlashAttribute("error", "该菜单已发布锁定，不可编辑；请使用“复制”生成新草稿");
			return "redirect:/menus";
		}
		MenuForm form = new MenuForm();
		form.setMenuId(menu.getMenuId());
		form.setName(menu.getName());
		form.setDescription(menu.getDescription());
		form.setEffectiveDate(menu.getEffectiveDate());
		form.setVersion(menu.getVersion());
		form.setRows(buildRows(menu));
		model.addAttribute("form", form);
		model.addAttribute("isEdit", true);
		return "menu/form";
	}

	/**
	 * 保存菜单（新建草稿或更新草稿）
	 *
	 * @param form     表单
	 * @param redirect 重定向属性
	 * @return 重定向
	 */
	@PostMapping("/save")
	public String save(@ModelAttribute("form") MenuForm form, RedirectAttributes redirect) {
		try {
			if (form.getMenuId() == null) {
				MenuCreateCmd cmd = new MenuCreateCmd();
				cmd.setName(form.getName());
				cmd.setDescription(form.getDescription());
				cmd.setEffectiveDate(form.getEffectiveDate() == null ? LocalDate.now() : form.getEffectiveDate());
				cmd.setItems(collectCreateItems(form));
				menuService.createDraft(cmd);
				redirect.addFlashAttribute("message", "菜单草稿创建成功");
			} else {
				MenuUpdateCmd cmd = new MenuUpdateCmd();
				cmd.setName(form.getName());
				cmd.setDescription(form.getDescription());
				cmd.setEffectiveDate(form.getEffectiveDate());
				cmd.setVersion(form.getVersion());
				cmd.setItems(collectUpdateItems(form));
				menuService.update(form.getMenuId(), cmd);
				redirect.addFlashAttribute("message", "菜单已更新");
			}
		} catch (RuntimeException exception) {
			redirect.addFlashAttribute("error", exception.getMessage());
			return form.getMenuId() == null
					? "redirect:/menus/new"
					: "redirect:/menus/" + form.getMenuId() + "/edit";
		}
		return "redirect:/menus";
	}

	/**
	 * 发布菜单：冻结快照并锁定，随后可供点餐
	 *
	 * @param id       菜单ID
	 * @param redirect 重定向属性
	 * @return 重定向
	 */
	@PostMapping("/{id}/publish")
	public String publish(@PathVariable("id") Long id, RedirectAttributes redirect) {
		try {
			var result = menuService.publish(id);
			redirect.addFlashAttribute("message",
					"菜单已发布，冻结 " + result.getSnapshotCount() + " 条价格快照");
		} catch (RuntimeException exception) {
			redirect.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/menus";
	}

	/**
	 * 下架菜单（已发布 → 已下架，快照与历史订单不受影响）
	 *
	 * @param id       菜单ID
	 * @param reason   下架原因
	 * @param redirect 重定向属性
	 * @return 重定向
	 */
	@PostMapping("/{id}/offline")
	public String unpublish(
			@PathVariable("id") Long id,
			@RequestParam(value = "reason", required = false) String reason,
			RedirectAttributes redirect) {
		try {
			menuService.unpublish(id, reason);
			redirect.addFlashAttribute("message", "菜单已下架");
		} catch (RuntimeException exception) {
			redirect.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/menus";
	}

	/**
	 * 复制历史菜单为新的可编辑草稿（历史菜单复用的正式入口）
	 * <p>
	 * 复制只读取源菜单的快照明细并生成新草稿，**不修改源菜单**，
	 * 因此已发布菜单“不可编辑”的约束不会被破坏。
	 *
	 * @param id       菜单ID
	 * @param redirect 重定向属性
	 * @return 重定向
	 */
	@PostMapping("/{id}/copy")
	public String copy(@PathVariable("id") Long id, RedirectAttributes redirect) {
		try {
			MenuVO source = findMenu(id);
			if (source == null) {
				throw new IllegalArgumentException("菜单不存在：" + id);
			}
			MenuCreateCmd cmd = new MenuCreateCmd();
			cmd.setName(source.getName() + "（副本）");
			cmd.setDescription(source.getDescription());
			cmd.setEffectiveDate(LocalDate.now());
			List<MenuCreateItem> items = new ArrayList<>();
			if (source.getItems() != null) {
				for (var item : source.getItems()) {
					MenuCreateItem createItem = new MenuCreateItem();
					createItem.setRecipeId(item.getRecipeId() != null ? item.getRecipeId() : item.getItemId());
					createItem.setMenuPrice(item.getMenuPrice() != null ? item.getMenuPrice() : item.getUnitPrice());
					items.add(createItem);
				}
			}
			cmd.setItems(items);
			Long newMenuId = menuService.createDraft(cmd);
			redirect.addFlashAttribute("message", "已复制为新的草稿菜单（ID=" + newMenuId + "），可继续调整");
		} catch (RuntimeException exception) {
			redirect.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/menus";
	}

	/**
	 * 把草稿菜单下架（不做物理删除，历史数据始终保留）
	 *
	 * @param id       菜单ID
	 * @param redirect 重定向属性
	 * @return 重定向
	 */
	@PostMapping("/{id}/delete")
	public String delete(@PathVariable("id") Long id, RedirectAttributes redirect) {
		try {
			menuService.unpublish(id, "页面删除草稿");
			redirect.addFlashAttribute("message", "菜单已下架（历史数据保留）");
		} catch (RuntimeException exception) {
			redirect.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/menus";
	}

	// ------------------------------------------------------------------ 内部方法

	/**
	 * 按ID取菜单（含明细）
	 *
	 * @param menuId 菜单ID
	 * @return 菜单视图；不存在返回 null
	 */
	private MenuVO findMenu(Long menuId) {
		MenuQuery query = new MenuQuery();
		query.setMenuId(menuId);
		query.setWithItems(Boolean.TRUE);
		PageResult<MenuVO> page = menuService.page(query);
		return page.getList().isEmpty() ? null : page.getList().get(0);
	}

	/**
	 * 收集勾选行（新增草稿用）
	 *
	 * @param form 表单
	 * @return 菜单条目
	 */
	private List<MenuCreateItem> collectCreateItems(MenuForm form) {
		List<MenuCreateItem> items = new ArrayList<>();
		for (MenuForm.Row row : form.getRows()) {
			if (!row.isSelected() || row.isDisabled()) {
				continue;
			}
			MenuCreateItem item = new MenuCreateItem();
			item.setRecipeId(row.getRecipeId());
			item.setMenuPrice(row.getMenuPrice());
			items.add(item);
		}
		return items;
	}

	/**
	 * 收集勾选行（更新草稿用）
	 *
	 * @param form 表单
	 * @return 菜单条目
	 */
	private List<MenuUpdateItem> collectUpdateItems(MenuForm form) {
		List<MenuUpdateItem> items = new ArrayList<>();
		for (MenuForm.Row row : form.getRows()) {
			if (!row.isSelected() || row.isDisabled()) {
				continue;
			}
			MenuUpdateItem item = new MenuUpdateItem();
			item.setRecipeId(row.getRecipeId());
			item.setMenuPrice(row.getMenuPrice());
			items.add(item);
		}
		return items;
	}

	/**
	 * 构建“全部菜品 + 勾选状态 + 菜单售价”的编辑网格
	 *
	 * @param menu 正在编辑的菜单，为 null 表示新建
	 * @return 编辑行
	 */
	private List<MenuForm.Row> buildRows(MenuVO menu) {
		Map<Long, MenuForm.Row> selected = new HashMap<>();
		if (menu != null && menu.getItems() != null) {
			for (var item : menu.getItems()) {
				MenuForm.Row row = new MenuForm.Row();
				row.setRecipeId(item.getRecipeId() != null ? item.getRecipeId() : item.getItemId());
				row.setSelected(true);
				row.setMenuPrice(item.getMenuPrice() != null ? item.getMenuPrice() : item.getUnitPrice());
				selected.put(row.getRecipeId(), row);
			}
		}
		RecipeQuery query = new RecipeQuery();
		query.setPageNum(1);
		query.setPageSize(QUERY_PAGE_SIZE);
		List<RecipeVO> recipes = recipeService.page(query).getList();
		List<MenuForm.Row> rows = new ArrayList<>();
		for (RecipeVO recipe : recipes) {
			boolean disabled = !RecipeStatus.ACTIVE.name().equals(recipe.getStatus());
			MenuForm.Row chosen = selected.get(recipe.getRecipeId());
			// 历史菜单里的菜品即使已停用也要展示出来，否则编辑时会莫名丢菜
			MenuForm.Row row = chosen == null ? new MenuForm.Row() : chosen;
			row.setRecipeId(recipe.getRecipeId());
			row.setRecipeName(recipe.getName());
			row.setCategory(recipe.getCategory());
			row.setUnit(recipe.getUnit());
			row.setDisabled(disabled);
			if (row.getMenuPrice() == null) {
				row.setMenuPrice(recipe.getUnitPrice());
			}
			rows.add(row);
		}
		return rows;
	}

	/**
	 * 菜单状态选项（供模板渲染，避免模板里硬编码状态文案）
	 *
	 * @return 状态数组
	 */
	@ModelAttribute("allMenuStatus")
	public MenuStatus[] allMenuStatus() {
		return MenuStatus.values();
	}
}
