package com.university.webdesign.menurecipe.web;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单表单数据。
 * rows 包含候选菜品，每行记录菜品ID、是否勾选以及菜单级售价。
 */
@Data
public class MenuFormData {

	/**
	 * 菜单ID（编辑时存在）
	 */
	private Long id;

	/**
	 * 菜单名称
	 */
	private String name;

	/**
	 * 菜单描述
	 */
	private String description;

	/**
	 * 候选菜品行
	 */
	private List<Row> rows = new ArrayList<>();

	@Data
	public static class Row {
		/**
		 * 菜品ID
		 */
		private Long recipeId;

		/**
		 * 菜品名称（展示用）
		 */
		private String recipeName;

		/**
		 * 分类（展示用）
		 */
		private String category;

		/**
		 * 单位（展示用）
		 */
		private String unit;

		/**
		 * 是否选中加入菜单
		 */
		private boolean selected;

		/**
		 * 菜单级售价
		 */
		private BigDecimal price;

		/**
		 * 菜品是否已停用（仅展示，已停用菜品不可再被新选）
		 */
		private boolean inactive;
	}
}
