package com.university.webdesign.menurecipe.web;

import com.university.webdesign.menurecipe.api.MenuDTO;
import com.university.webdesign.menurecipe.api.MenuItemDTO;
import com.university.webdesign.menurecipe.api.RecipeDTO;
import com.university.webdesign.menurecipe.service.MenuService;
import com.university.webdesign.menurecipe.service.RecipeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单管理页面控制器（服务端渲染）。
 * 负责菜单列表、组装/编辑菜单（选择菜品并定价）、发布/下架/复制等操作。
 */
@Controller
@RequestMapping("/menus")
public class MenuPageController {

	private final MenuService menuService;
	private final RecipeService recipeService;

	public MenuPageController(MenuService menuService, RecipeService recipeService) {
		this.menuService = menuService;
		this.recipeService = recipeService;
	}

	/**
	 * 菜单列表
	 */
	@GetMapping
	public String list(Model model) {
		model.addAttribute("menus", menuService.getAll());
		return "menu/list";
	}

	/**
	 * 新建菜单表单
	 */
	@GetMapping("/new")
	public String newForm(Model model) {
		MenuFormData form = new MenuFormData();
		form.setRows(buildRows(null));
		model.addAttribute("form", form);
		model.addAttribute("isEdit", false);
		return "menu/form";
	}

	/**
	 * 编辑菜单表单
	 */
	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirect) {
		MenuDTO menu = menuService.getById(id);
		if (menu.isLocked()) {
			redirect.addFlashAttribute("error", "历史菜单已锁定，不可编辑；请使用“复制”生成新菜单");
			return "redirect:/menus";
		}
		MenuFormData form = new MenuFormData();
		form.setId(menu.getId());
		form.setName(menu.getName());
		form.setDescription(menu.getDescription());
		form.setRows(buildRows(menu));
		model.addAttribute("form", form);
		model.addAttribute("isEdit", true);
		return "menu/form";
	}

	/**
	 * 保存菜单（新建或更新）
	 */
	@PostMapping("/save")
	public String save(@ModelAttribute("form") MenuFormData form, RedirectAttributes redirect) {
		try {
			MenuDTO dto = new MenuDTO();
			dto.setId(form.getId());
			dto.setName(form.getName());
			dto.setDescription(form.getDescription());

			List<MenuItemDTO> items = form.getRows().stream()
					.filter(MenuFormData.Row::isSelected)
					.map(row -> {
						MenuItemDTO item = new MenuItemDTO();
						item.setRecipeId(row.getRecipeId());
						item.setPrice(row.getPrice());
						return item;
					})
					.toList();
			dto.setMenuItems(items);

			if (form.getId() == null) {
				menuService.create(dto);
				redirect.addFlashAttribute("message", "菜单创建成功（草稿）");
			} else {
				menuService.update(dto);
				redirect.addFlashAttribute("message", "菜单修改成功");
			}
		} catch (IllegalArgumentException | IllegalStateException e) {
			redirect.addFlashAttribute("error", e.getMessage());
			return form.getId() == null
					? "redirect:/menus/new"
					: "redirect:/menus/" + form.getId() + "/edit";
		}
		return "redirect:/menus";
	}

	/**
	 * 删除菜单
	 */
	@PostMapping("/{id}/delete")
	public String delete(@PathVariable("id") Long id, RedirectAttributes redirect) {
		try {
			menuService.delete(id);
			redirect.addFlashAttribute("message", "菜单已删除");
		} catch (IllegalStateException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/menus";
	}

	/**
	 * 发布菜单
	 */
	@PostMapping("/{id}/publish")
	public String publish(@PathVariable("id") Long id, RedirectAttributes redirect) {
		try {
			menuService.publish(id);
			redirect.addFlashAttribute("message", "菜单已发布");
		} catch (IllegalStateException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/menus";
	}

	/**
	 * 下架菜单
	 */
	@PostMapping("/{id}/offline")
	public String offline(@PathVariable("id") Long id, RedirectAttributes redirect) {
		menuService.offline(id);
		redirect.addFlashAttribute("message", "菜单已下架");
		return "redirect:/menus";
	}

	/**
	 * 复制菜单
	 */
	@PostMapping("/{id}/copy")
	public String copy(@PathVariable("id") Long id, RedirectAttributes redirect) {
		menuService.copyMenu(id);
		redirect.addFlashAttribute("message", "已复制该菜单为草稿");
		return "redirect:/menus";
	}

	/**
	 * 根据所有菜品构建表单行；若正在编辑，按已有菜单项标记选中与价格。
	 */
	private List<MenuFormData.Row> buildRows(MenuDTO menu) {
		Map<Long, MenuItemDTO> existing = new HashMap<>();
		if (menu != null && menu.getMenuItems() != null) {
			for (MenuItemDTO item : menu.getMenuItems()) {
				existing.put(item.getRecipeId(), item);
			}
		}
		List<RecipeDTO> recipes = recipeService.getAll();
		return recipes.stream().map(recipe -> {
			MenuFormData.Row row = new MenuFormData.Row();
			row.setRecipeId(recipe.getRecipeId());
			row.setRecipeName(recipe.getRecipeName());
			row.setCategory(recipe.getCategory());
			row.setUnit(recipe.getUnit());
			row.setPrice(recipe.getPrice());
			row.setInactive(!"ACTIVE".equals(recipe.getStatus()));
			MenuItemDTO chosen = existing.get(recipe.getRecipeId());
			if (chosen != null) {
				row.setSelected(true);
				row.setPrice(chosen.getPrice());
			}
			return row;
		}).toList();
	}
}
