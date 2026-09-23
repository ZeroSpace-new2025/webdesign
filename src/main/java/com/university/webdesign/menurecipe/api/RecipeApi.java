package com.university.webdesign.menurecipe.api;

import com.university.webdesign.common.Result;
import com.university.webdesign.menurecipe.service.RecipeService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/*其他的API接口可以根据需要添加。修改后要立即提交。*/
/*应该只包含与菜谱相关的API接口，而不应该包含与其他相关的接口。*/
/*请勿把内部实现暴露给外部。*/

/**
 * 菜谱API接口
 *
 */
@RestController
@RequestMapping("/api/recipe")
public class RecipeApi
{
	public RecipeApi(RecipeService recipeService) {
		this.recipeService = recipeService;
	}
	
	private final RecipeService recipeService;
	
	@GetMapping
	public Result<RecipeDTO> getRecipe() {
		//todo: implement the logic to retrieve recipe data from the database and return it as a JSON response
		return Result.success(new RecipeDTO());
	}
	
	@PostMapping("query")
	public Result<List<RecipeDTO>> query(@RequestBody RecipeQueryData queryData) {
		//todo: implement the logic to query recipe data based on the provided parameters
		return Result.success(new ArrayList<>());
	}
	
	@GetMapping("all")
	public Result<List<RecipeDTO>> getAllRecipes() {
		//todo: implement the logic to retrieve all recipe data from the database and return it as a JSON response
		return Result.success(new ArrayList<>());
	}
	//other API
}
