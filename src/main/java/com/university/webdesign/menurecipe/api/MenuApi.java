package com.university.webdesign.menurecipe.api;

import com.university.webdesign.common.Result;
import com.university.webdesign.menurecipe.service.MenuService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
/*其他的API接口可以根据需要添加。修改后要立即提交。*/
/*应该只包含与菜单相关的API接口，而不应该包含与其他相关的接口。*/
/*请勿把内部实现暴露给外部。*/

/**
 * 菜单API接口
 *
 */
@RestController
@RequestMapping("/api/menu")
public class MenuApi
{
	public MenuApi(MenuService menuService) {
		this.menuService = menuService;
	}
	
	private final MenuService menuService;
	
	@GetMapping
	public Result<MenuDTO> getMenu() {
		//todo: implement the logic to retrieve menu data from the database and return it as a JSON response
		return Result.success(new MenuDTO());
	}
	
	@PostMapping("query")
	public Result<List<MenuDTO>> query(@RequestBody MenuQueryData queryData) {
		//todo: implement the logic to query menu data based on the provided parameters
		return Result.success(new ArrayList<>());
	}
	
	@GetMapping("all")
	public Result<List<MenuDTO>> getAllMenus() {
		//todo: implement the logic to retrieve all menu data from the database and return it as a JSON response
		return Result.success(new ArrayList<>());
	}
	//other API
}