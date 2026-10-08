package com.university.webdesign.domain.menu;

/**
 * 菜单状态。
 * <p>
 * 对应《对外方法表》M1 的 {@code MenuStatus}：
 * {@link #DRAFT}（草稿，可反复修改）→ {@link #PUBLISHED}（已发布，冻结快照并锁定）
 * → {@link #OFFLINE}（已下架，不再接单）。
 * <p>
 * 状态流转约束：只有 {@code DRAFT} 可改；{@code PUBLISHED} 后 {@code locked=true}
 * 不可改不可删，只能 {@code unpublish} 下架。
 */
public enum MenuStatus
{
	/**
	 * 草稿：可增删菜品、调价、改生效日期
	 */
	DRAFT("草稿"),

	/**
	 * 已发布：已冻结快照并锁定，供点餐取价
	 */
	PUBLISHED("已发布"),

	/**
	 * 已下架：不再接单，历史订单与快照保留
	 */
	OFFLINE("已下架");

	/**
	 * 中文展示文案
	 */
	private final String label;

	MenuStatus(String label) {
		this.label = label;
	}

	/**
	 * @return 中文展示文案
	 */
	public String getLabel() {
		return label;
	}
}
