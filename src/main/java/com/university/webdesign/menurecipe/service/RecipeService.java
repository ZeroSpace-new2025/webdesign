package com.university.webdesign.menurecipe.service;

import com.university.webdesign.menurecipe.api.RecipeDTO;
import com.university.webdesign.menurecipe.api.RecipeQueryData;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 菜谱服务（跨模块能力契约）
 * <p>
 * 实现类放在同级的 {@code impl} 包下，命名 {@code RecipeServiceImpl}。
 * <p>
 * //todo 确认：本接口的方法签名按 {@code menurecipe.api.RecipeApi} 的调用反推而来，
 * 请菜品与菜单中心（方家乐）确认后补齐实现（食谱增删查改、图片上传、数据保护机制）。
 */
@Component
public interface RecipeService
{
	/**
	 * 获取指定菜谱
	 *
	 * @param recipeId 菜谱ID
	 * @return 菜谱；不存在时返回 null
	 */
	RecipeDTO getRecipe(Long recipeId);
	
	/**
	 * 获取全部菜谱
	 *
	 * @return 菜谱列表
	 * 根据ID获取菜品
	 */
	List<RecipeDTO> getAllRecipes();
	
	RecipeDTO getById(Long id);

	/**
	 * 按条件查询菜谱
	 *
	 * @param queryData 查询条件
	 * @return 菜谱列表
	 * 按条件查询菜品
	 */
	List<RecipeDTO> query(RecipeQueryData queryData);

	/**
	 * 获取全部菜品
	 */
	List<RecipeDTO> getAll();

	/**
	 * 新增菜品
	 */
	RecipeDTO create(RecipeDTO recipeDTO);

	/**
	 * 修改菜品
	 */
	RecipeDTO update(RecipeDTO recipeDTO);

	/**
	 * 删除菜品（逻辑停用，保留历史数据）
	 */
	void delete(Long id);

	/**
	 * 上传菜品图片，返回可访问的图片地址
	 */
	String uploadImage(MultipartFile file);
}
