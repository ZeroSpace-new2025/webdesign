package com.university.webdesign.menurecipe.service;

import com.university.webdesign.menurecipe.api.RecipeDTO;
import com.university.webdesign.menurecipe.api.RecipeQueryData;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 菜品（食谱）服务。
 * 负责菜品的增删查改、图片上传以及数据保护。
 */
public interface RecipeService {

	/**
	 * 根据ID获取菜品
	 */
	RecipeDTO getById(Long id);

	/**
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
