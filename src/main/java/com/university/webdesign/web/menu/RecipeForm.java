package com.university.webdesign.web.menu;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

/**
 * 菜品表单（页面层入参）。
 * <p>
 * 页面表单字段与 {@code RecipeCreateCmd} / {@code RecipeUpdateCmd} 分开：页面需要接收
 * {@link #imageFile}（multipart 上传）与 {@link #version}（乐观锁回传），
 * 由页面控制器转成服务层命令对象后再调用 {@code RecipeService}。
 */
@Data
public class RecipeForm
{
	/**
	 * 菜品ID，为空表示新增
	 */
	private Long recipeId;

	/**
	 * 菜品名称
	 */
	private String name;

	/**
	 * 分类名
	 */
	private String category;

	/**
	 * 计量单位
	 */
	private String unit;

	/**
	 * 标准单价（元）
	 */
	private BigDecimal unitPrice;

	/**
	 * 菜品描述
	 */
	private String description;

	/**
	 * 状态：ACTIVE / DISABLED
	 */
	private String status;

	/**
	 * 已保存的图片地址
	 */
	private String imageUrl;

	/**
	 * 乐观锁版本号，编辑时回传
	 */
	private Integer version;

	/**
	 * 待上传的图片文件，可为空（为空表示不改动图片）
	 */
	private MultipartFile imageFile;
}
