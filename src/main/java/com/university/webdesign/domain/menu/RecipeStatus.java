package com.university.webdesign.domain.menu;

/**
 * 菜品（食谱）状态。
 * <p>
 * 对应《对外方法表》M1 的 {@code RecipeStatus}：
 * {@link #ACTIVE}（可售，可加入新菜单）与 {@link #DISABLED}（逻辑下架，不可加入新菜单）。
 * <p>
 * 下架只改本表状态、不做物理删除：已发布菜单有冻结快照、历史订单有冗余明细，
 * 因此菜品下架只影响未来菜单，不影响历史数据。
 */
public enum RecipeStatus
{
	/**
	 * 可售：可用于生成新菜单
	 */
	ACTIVE("可售"),

	/**
	 * 已下架：不再用于新菜单，历史快照与历史订单不受影响
	 */
	DISABLED("已下架");

	/**
	 * 中文展示文案
	 */
	private final String label;

	RecipeStatus(String label) {
		this.label = label;
	}

	/**
	 * @return 中文展示文案
	 */
	public String getLabel() {
		return label;
	}
}
