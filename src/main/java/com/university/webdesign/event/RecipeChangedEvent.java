package com.university.webdesign.event;

import java.time.LocalDateTime;

/**
 * 菜品变更事件（M1 发布，M3 订阅）。
 * <p>
 * 对应《对外方法表》2.3：菜品新增/修改/下架后发布，运营履约据此标记统计口径需重建。
 *
 * @param recipeId   菜品ID
 * @param changeType 变更类型：CREATED / UPDATED / DISABLED
 * @param occurredAt 发生时间
 */
public record RecipeChangedEvent(Long recipeId, String changeType, LocalDateTime occurredAt)
{
	/**
	 * 变更类型常量
	 */
	public static final String CREATED = "CREATED";

	/**
	 * 变更类型常量
	 */
	public static final String UPDATED = "UPDATED";

	/**
	 * 变更类型常量
	 */
	public static final String DISABLED = "DISABLED";
}
