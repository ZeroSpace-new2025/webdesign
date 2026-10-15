package com.university.webdesign.api.menu;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.Result;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.config.RequiresPerm;
import com.university.webdesign.service.menu.RecipeService;
import com.university.webdesign.service.menu.dto.CategoryVO;
import com.university.webdesign.service.menu.dto.RecipeCreateCmd;
import com.university.webdesign.service.menu.dto.RecipeQuery;
import com.university.webdesign.service.menu.dto.RecipeUpdateCmd;
import com.university.webdesign.service.menu.dto.RecipeVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 食谱管理 REST 接口（M1-01 ~ M1-07），基础路径 {@code /api/v1/menu/recipes}。
 * <p>
 * 分层约定：本类只做 HTTP 协议转换与 {@code Result} 组装，不写任何业务判断；
 * 业务规则、事务边界全部在 {@code RecipeService} 实现中，失败由 service 抛
 * {@code BusinessException}，再由全局异常处理器统一映射成 {@code Result}。
 * <p>
 * 写操作按《对外方法表》M1 的角色要求用 {@link RequiresPerm} 声明权限点
 * （{@code menu:recipe:manage}，或餐厅经理/厨房主管角色），由 {@code AuthInterceptor} 统一校验；
 * 查询接口对所有已登录角色开放。
 * <p>
 * 身份不再由请求体携带：操作人取自登录上下文（{@code AuthInterceptor} 已写入
 * {@code UserContextHolder}）。
 */
@RestController
@RequestMapping("/api/v1/menu/recipes")
public class ApiRecipeController
{
	private final RecipeService recipeService;

	public ApiRecipeController(RecipeService recipeService) {
		this.recipeService = recipeService;
	}

	/**
	 * 新增菜品（M1-01）
	 *
	 * @param cmd 新增请求（name、categoryId、imageUrl、unit、unitPrice、description）
	 * @return 菜品ID + 菜品 VO
	 */
	@PostMapping
	@RequiresPerm(value = PermissionEnum.MENU_RECIPE_MANAGE, roles = {RoleCodes.MANAGER, RoleCodes.KITCHEN_SUPERVISOR})
	public Result<RecipeVO> create(@Valid @RequestBody RecipeCreateCmd cmd) {
		Long recipeId = recipeService.create(cmd);
		return Result.success(recipeService.getById(recipeId));
	}

	/**
	 * 更新菜品（M1-02）
	 *
	 * @param recipeId 菜品ID
	 * @param cmd      更新请求（含 version 乐观锁）
	 * @return 更新后的菜品 VO
	 */
	@PutMapping("/{recipeId}")
	@RequiresPerm(value = PermissionEnum.MENU_RECIPE_MANAGE, roles = {RoleCodes.MANAGER, RoleCodes.KITCHEN_SUPERVISOR})
	public Result<RecipeVO> update(@PathVariable("recipeId") Long recipeId,
			@Valid @RequestBody RecipeUpdateCmd cmd) {
		recipeService.update(recipeId, cmd);
		return Result.success(recipeService.getById(recipeId));
	}

	/**
	 * 逻辑下架菜品（M1-03）
	 *
	 * @param recipeId 菜品ID
	 * @param reason   下架原因，可选
	 * @return 受影响菜单数（被已发布菜单引用时抛 42203）
	 */
	@DeleteMapping("/{recipeId}")
	@RequiresPerm(value = PermissionEnum.MENU_RECIPE_MANAGE, roles = {RoleCodes.MANAGER, RoleCodes.KITCHEN_SUPERVISOR})
	public Result<Integer> disable(
			@PathVariable("recipeId") Long recipeId,
			@RequestParam(value = "reason", required = false) String reason) {
		return Result.success(recipeService.disable(recipeId, reason));
	}

	/**
	 * 查询菜品详情（M1-04）
	 *
	 * @param recipeId 菜品ID
	 * @return 菜品 VO
	 */
	@GetMapping("/{recipeId}")
	public Result<RecipeVO> detail(@PathVariable("recipeId") Long recipeId) {
		return Result.success(recipeService.getById(recipeId));
	}

	/**
	 * 分页查询菜品（M1-05）
	 *
	 * @param query 查询条件（keyword、categoryId、status、minPrice、maxPrice + 分页）
	 * @return 分页菜品
	 */
	@GetMapping
	public Result<PageResult<RecipeVO>> page(@ModelAttribute RecipeQuery query) {
		return Result.success(recipeService.page(query));
	}

	/**
	 * 上传菜品图片（M1-06）
	 *
	 * @param file multipart/form-data 中的文件字段 {@code file}
	 * @return 图片地址与存储键
	 */
	@PostMapping("/images")
	@RequiresPerm(value = PermissionEnum.MENU_RECIPE_MANAGE, roles = {RoleCodes.MANAGER, RoleCodes.KITCHEN_SUPERVISOR})
	public Result<ImageUploadVO> uploadImage(@RequestParam("file") MultipartFile file) {
		return Result.success(ImageUploadVO.of(recipeService.uploadImage(file)));
	}

	/**
	 * 查询菜品分类（M1-07）
	 *
	 * @return 分类列表（当前由 {@code recipe.category} 去重派生）
	 */
	@GetMapping("/categories")
	public Result<List<CategoryVO>> categories() {
		return Result.success(recipeService.listCategories());
	}
}
