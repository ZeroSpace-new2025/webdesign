package com.university.webdesign.web.menu;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单表单（页面层入参）。
 * <p>
 * {@link #rows} 是“全部菜品 + 勾选状态 + 菜单售价”的编辑网格，
 * 保存时把勾选行转成 {@code MenuCreateItem} / {@code MenuUpdateItem}。
 */
@Data
public class MenuForm
{
	/**
	 * 菜单ID，为空表示新建
	 */
	private Long menuId;

	/**
	 * 菜单名称
	 */
	private String name;

	/**
	 * 菜单描述
	 */
	private String description;

	/**
	 * 生效日期
	 */
	private LocalDate effectiveDate;

	/**
	 * 乐观锁版本号，编辑时回传
	 */
	private Integer version;

	/**
	 * 菜品编辑行
	 */
	private List<Row> rows = new ArrayList<>();

	/**
	 * 菜品编辑行
	 */
	@Data
	public static class Row
	{
		/**
		 * 菜品ID
		 */
		private Long recipeId;

		/**
		 * 是否勾选进入菜单
		 */
		private boolean selected;

		/**
		 * 菜品名称（只读展示）
		 */
		private String recipeName;

		/**
		 * 分类（只读展示）
		 */
		private String category;

		/**
		 * 计量单位（只读展示）
		 */
		private String unit;

		/**
		 * 菜单售价
		 */
		private BigDecimal menuPrice;

		/**
		 * 菜品是否已停用（停用菜品不可加入新菜单）
		 */
		private boolean disabled;
	}
}
