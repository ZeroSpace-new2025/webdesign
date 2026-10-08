package com.university.webdesign.service.menu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品视图对象（M1-01/M1-02/M1-04/M1-05 的统一读模型）。
 * <p>
 * 字段名与《对外方法表》M1 的接口入参保持一致（{@code name}/{@code unitPrice}），
 * 便于 api 层直接回显。{@link #status} 为枚举名（{@code ACTIVE}/{@code DISABLED}），
 * {@link #statusText} 为中文文案。
 */
@Data
public class RecipeVO
{
	/**
	 * 菜品ID
	 */
	private Long recipeId;

	/**
	 * 菜品名称
	 */
	private String name;

	/**
	 * 分类ID（派生分类哈希）
	 */
	private Long categoryId;

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
	 * 菜品图片地址
	 */
	private String imageUrl;

	/**
	 * 菜品描述
	 */
	private String description;

	/**
	 * 状态编码：ACTIVE（可售）/ DISABLED（已下架）
	 */
	private String status;

	/**
	 * 状态中文文案
	 */
	private String statusText;

	/**
	 * 乐观锁版本号
	 */
	private Integer version;

	/**
	 * 创建者用户ID
	 */
	private Long createdBy;

	/**
	 * 创建时间
	 */
	private LocalDateTime createdAt;

	/**
	 * 最后修改时间
	 */
	private LocalDateTime updatedAt;
}
