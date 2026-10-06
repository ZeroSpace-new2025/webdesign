package com.university.webdesign.menurecipe.web;

import com.university.webdesign.menurecipe.api.RecipeDTO;
import com.university.webdesign.menurecipe.service.RecipeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 菜品管理页面控制器（服务端渲染）。
 * 负责菜品列表、新增/编辑表单与删除操作，复用 RecipeService 业务逻辑。
 */
@Controller
@RequestMapping("/recipes")
public class RecipePageController {

	private static final List<String> CATEGORIES = List.of("荤菜", "素菜", "主食", "汤品", "饮品", "其他");

	private final RecipeService recipeService;

	public RecipePageController(RecipeService recipeService) {
		this.recipeService = recipeService;
	}

	/**
	 * 菜品列表
	 */
	@GetMapping
	public String list(Model model) {
		model.addAttribute("recipes", recipeService.getAll());
		return "recipe/list";
	}

	/**
	 * 新增菜品表单
	 */
	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("recipe", new RecipeDTO());
		model.addAttribute("categories", CATEGORIES);
		model.addAttribute("isEdit", false);
		return "recipe/form";
	}

	/**
	 * 编辑菜品表单
	 */
	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable("id") Long id, Model model) {
		model.addAttribute("recipe", recipeService.getById(id));
		model.addAttribute("categories", CATEGORIES);
		model.addAttribute("isEdit", true);
		return "recipe/form";
	}

	/**
	 * 保存菜品（新建或更新）
	 */
	@PostMapping("/save")
	public String save(@ModelAttribute("recipe") RecipeDTO recipe,
			@RequestParam(value = "image", required = false) MultipartFile image,
			RedirectAttributes redirect) {
		try {
			// 若上传了新图片，先上传得到地址
			if (image != null && !image.isEmpty()) {
				String url = recipeService.uploadImage(image);
				recipe.setRecipeImageUrl(url);
			}
			if (recipe.getRecipeId() == null) {
				recipeService.create(recipe);
				redirect.addFlashAttribute("message", "菜品新增成功");
			} else {
				recipeService.update(recipe);
				redirect.addFlashAttribute("message", "菜品修改成功");
			}
		} catch (IllegalArgumentException e) {
			redirect.addFlashAttribute("error", e.getMessage());
			return recipe.getRecipeId() == null
					? "redirect:/recipes/new"
					: "redirect:/recipes/" + recipe.getRecipeId() + "/edit";
		}
		return "redirect:/recipes";
	}

	/**
	 * 删除菜品（逻辑停用）
	 */
	@PostMapping("/{id}/delete")
	public String delete(@PathVariable("id") Long id, RedirectAttributes redirect) {
		recipeService.delete(id);
		redirect.addFlashAttribute("message", "菜品已停用（历史数据保留）");
		return "redirect:/recipes";
	}
}
