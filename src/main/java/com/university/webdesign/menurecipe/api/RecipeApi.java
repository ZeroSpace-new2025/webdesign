package com.university.webdesign.menurecipe.api;

import com.university.webdesign.common.Result;
import com.university.webdesign.menurecipe.service.RecipeService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/*其他的API接口可以根据需要添加。修改后要立即提交。*/
/*应该只包含与菜谱相关的API接口，而不应该包含与其他相关的接口。*/
/*请勿把内部实现暴露给外部。*/

/**
 * 菜谱（菜品）API
 */
@RestController
@RequestMapping("/api/recipe")
public class RecipeApi {

	private final RecipeService recipeService;

	public RecipeApi(RecipeService recipeService) {
		this.recipeService = recipeService;
	}

	/**
	 * 根据ID获取菜品
	 */
	@GetMapping("/{id}")
	public Result<RecipeDTO> getRecipe(@PathVariable("id") Long id) {
		return Result.success(recipeService.getById(id));
	}

	/**
	 * 按条件查询菜品
	 */
	@PostMapping("/query")
	public Result<List<RecipeDTO>> query(@RequestBody RecipeQueryData queryData) {
		return Result.success(recipeService.query(queryData));
	}

	/**
	 * 获取全部菜品
	 */
	@GetMapping("/all")
	public Result<List<RecipeDTO>> getAllRecipes() {
		return Result.success(recipeService.getAll());
	}

	/**
	 * 新增菜品
	 */
	@PostMapping("/create")
	public Result<RecipeDTO> create(@RequestBody RecipeDTO recipeDTO) {
		return Result.success(recipeService.create(recipeDTO));
	}

	/**
	 * 修改菜品
	 */
	@PutMapping("/update")
	public Result<RecipeDTO> update(@RequestBody RecipeDTO recipeDTO) {
		return Result.success(recipeService.update(recipeDTO));
	}

	/**
	 * 删除菜品（逻辑停用，保留历史数据）
	 */
	@DeleteMapping("/{id}")
	public Result<Void> delete(@PathVariable("id") Long id) {
		recipeService.delete(id);
		return Result.success(null);
	}

	/**
	 * 上传菜品图片，返回图片地址
	 */
	@PostMapping("/image")
	public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
		return Result.success(recipeService.uploadImage(file));
	}
}
