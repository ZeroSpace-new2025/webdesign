package com.university.webdesign.web.menu;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.domain.menu.RecipeStatus;
import com.university.webdesign.service.menu.RecipeService;
import com.university.webdesign.service.menu.dto.CategoryVO;
import com.university.webdesign.service.menu.dto.RecipeCreateCmd;
import com.university.webdesign.service.menu.dto.RecipeQuery;
import com.university.webdesign.service.menu.dto.RecipeUpdateCmd;
import com.university.webdesign.service.menu.dto.RecipeVO;
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
 * 菜品管理页面控制器（服务端渲染，走 {@code /recipes/**}）。
 * <p>
 * 与 REST 控制器 {@code api.menu.ApiRecipeController} 分开写：本类只组装视图，
 * 业务规则（重名 40901、被已发布菜单引用 42203、乐观锁 40902）全部由 {@link RecipeService} 判定，
 * 页面只负责把失败原因回显。
 */
@Controller
@RequestMapping("/recipes")
public class RecipePageController
{
	private final RecipeService recipeService;

	public RecipePageController(RecipeService recipeService) {
		this.recipeService = recipeService;
	}

	/**
	 * 菜品列表
	 *
	 * @param keyword 名称关键字，可选
	 * @param status  状态筛选，可选
	 * @param model   视图模型
	 * @return 视图名
	 */
	@GetMapping
	public String list(
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "status", required = false) String status,
			Model model) {
		RecipeQuery query = new RecipeQuery();
		query.setKeyword(keyword);
		query.setStatus(status);
		query.setPageNum(1);
		query.setPageSize(100);
		PageResult<RecipeVO> page = recipeService.page(query);
		model.addAttribute("recipes", page.getList());
		model.addAttribute("total", page.getTotal());
		model.addAttribute("keyword", keyword);
		model.addAttribute("status", status);
		return "recipe/list";
	}

	/**
	 * 新增菜品表单
	 *
	 * @param model 视图模型
	 * @return 视图名
	 */
	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("form", new RecipeForm());
		model.addAttribute("categories", categoryNames());
		model.addAttribute("isEdit", false);
		return "recipe/form";
	}

	/**
	 * 编辑菜品表单
	 *
	 * @param id    菜品ID
	 * @param model 视图模型
	 * @return 视图名
	 */
	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable("id") Long id, Model model) {
		RecipeVO recipe = recipeService.getById(id);
		RecipeForm form = new RecipeForm();
		form.setRecipeId(recipe.getRecipeId());
		form.setName(recipe.getName());
		form.setCategory(recipe.getCategory());
		form.setUnit(recipe.getUnit());
		form.setUnitPrice(recipe.getUnitPrice());
		form.setDescription(recipe.getDescription());
		form.setStatus(recipe.getStatus());
		form.setImageUrl(recipe.getImageUrl());
		form.setVersion(recipe.getVersion());
		model.addAttribute("form", form);
		model.addAttribute("categories", categoryNames());
		model.addAttribute("isEdit", true);
		return "recipe/form";
	}

	/**
	 * 保存菜品（新增或更新）
	 *
	 * @param form     表单
	 * @param redirect 重定向属性
	 * @return 重定向
	 */
	@PostMapping("/save")
	public String save(@ModelAttribute("form") RecipeForm form, RedirectAttributes redirect) {
		try {
			String imageUrl = form.getImageUrl();
			MultipartFile image = form.getImageFile();
			if (image != null && !image.isEmpty()) {
				imageUrl = recipeService.uploadImage(image);
			}
			if (form.getRecipeId() == null) {
				RecipeCreateCmd cmd = new RecipeCreateCmd();
				cmd.setName(form.getName());
				cmd.setCategory(form.getCategory());
				cmd.setImageUrl(imageUrl);
				cmd.setUnit(form.getUnit());
				cmd.setUnitPrice(form.getUnitPrice());
				cmd.setDescription(form.getDescription());
				recipeService.create(cmd);
				redirect.addFlashAttribute("message", "菜品创建成功");
			} else {
				RecipeUpdateCmd cmd = new RecipeUpdateCmd();
				cmd.setName(form.getName());
				cmd.setCategory(form.getCategory());
				cmd.setImageUrl(imageUrl);
				cmd.setUnit(form.getUnit());
				cmd.setUnitPrice(form.getUnitPrice());
				cmd.setDescription(form.getDescription());
				cmd.setStatus(form.getStatus());
				cmd.setVersion(form.getVersion());
				recipeService.update(form.getRecipeId(), cmd);
				redirect.addFlashAttribute("message", "菜品修改成功");
			}
		} catch (RuntimeException exception) {
			redirect.addFlashAttribute("error", exception.getMessage());
			return form.getRecipeId() == null
					? "redirect:/recipes/new"
					: "redirect:/recipes/" + form.getRecipeId() + "/edit";
		}
		return "redirect:/recipes";
	}

	/**
	 * 逻辑下架菜品（不做物理删除，保护历史菜单与订单）
	 *
	 * @param id       菜品ID
	 * @param redirect 重定向属性
	 * @return 重定向
	 */
	@PostMapping("/{id}/delete")
	public String disable(@PathVariable("id") Long id, RedirectAttributes redirect) {
		try {
			int affected = recipeService.disable(id, "页面操作下架");
			redirect.addFlashAttribute("message", "菜品已停用，影响 " + affected + " 份草稿菜单");
		} catch (RuntimeException exception) {
			redirect.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/recipes";
	}

	/**
	 * 分类字典（供表单下拉）
	 *
	 * @return 分类名列表
	 */
	private List<String> categoryNames() {
		return recipeService.listCategories().stream().map(CategoryVO::getCategoryName).toList();
	}

	/**
	 * 状态选项（供模板渲染下拉，避免模板里硬编码枚举）
	 *
	 * @return 状态数组
	 */
	@ModelAttribute("allStatus")
	public RecipeStatus[] allStatus() {
		return RecipeStatus.values();
	}
}
