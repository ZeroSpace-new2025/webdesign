package com.university.webdesign.menurecipe.api;

import com.university.webdesign.common.Result;
import com.university.webdesign.menurecipe.service.MenuService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/*其他的API接口可以根据需要添加。修改后要立即提交。*/
/*应该只包含与菜单相关的API接口，而不应该包含与其他相关的接口。*/
/*请勿把内部实现暴露给外部。*/

/**
 * 菜单 API
 */
@RestController
@RequestMapping("/api/menu")
public class MenuApi {

	private final MenuService menuService;

	public MenuApi(MenuService menuService) {
		this.menuService = menuService;
	}

	/**
	 * 根据ID获取菜单
	 */
	@GetMapping("/{id}")
	public Result<MenuDTO> getMenu(@PathVariable("id") Long id) {
		return Result.success(menuService.getById(id));
	}

	/**
	 * 按条件查询菜单
	 */
	@PostMapping("/query")
	public Result<List<MenuDTO>> query(@RequestBody MenuQueryData queryData) {
		return Result.success(menuService.query(queryData));
	}

	/**
	 * 获取全部菜单
	 */
	@GetMapping("/all")
	public Result<List<MenuDTO>> getAllMenus() {
		return Result.success(menuService.getAll());
	}

	/**
	 * 新建菜单（可携带菜单项）
	 */
	@PostMapping("/create")
	public Result<MenuDTO> create(@RequestBody MenuDTO menuDTO) {
		return Result.success(menuService.create(menuDTO));
	}

	/**
	 * 修改菜单
	 */
	@PutMapping("/update")
	public Result<MenuDTO> update(@RequestBody MenuDTO menuDTO) {
		return Result.success(menuService.update(menuDTO));
	}

	/**
	 * 删除菜单
	 */
	@DeleteMapping("/{id}")
	public Result<MenuDTO> delete(@PathVariable("id") Long id) {
		menuService.delete(id);
		return Result.success(null);
	}

	/**
	 * 发布菜单
	 */
	@PutMapping("/{id}/publish")
	public Result<MenuDTO> publish(@PathVariable("id") Long id) {
		return Result.success(menuService.publish(id));
	}

	/**
	 * 下架菜单
	 */
	@PutMapping("/{id}/offline")
	public Result<MenuDTO> offline(@PathVariable("id") Long id) {
		return Result.success(menuService.offline(id));
	}

	/**
	 * 复制历史菜单，生成可编辑的草稿副本
	 */
	@PostMapping("/{id}/copy")
	public Result<MenuDTO> copy(@PathVariable("id") Long id) {
		return Result.success(menuService.copyMenu(id));
	}

	/**
	 * 在菜单层级调整某菜品的售价
	 */
	@PutMapping("/{id}/item/{itemId}/price")
	public Result<MenuDTO> adjustItemPrice(@PathVariable("id") Long menuId,
			@PathVariable("itemId") Long itemId,
			@RequestParam("price") BigDecimal price) {
		return Result.success(menuService.adjustItemPrice(menuId, itemId, price));
	}
}
