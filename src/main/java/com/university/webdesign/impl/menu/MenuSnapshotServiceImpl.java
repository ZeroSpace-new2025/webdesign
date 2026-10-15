package com.university.webdesign.impl.menu;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.domain.menu.MenuItem;
import com.university.webdesign.domain.menu.MenuSnapshot;
import com.university.webdesign.domain.menu.Recipe;
import com.university.webdesign.repository.menu.MenuItemRepository;
import com.university.webdesign.repository.menu.MenuRepository;
import com.university.webdesign.repository.menu.MenuSnapshotRepository;
import com.university.webdesign.repository.menu.RecipeRepository;
import com.university.webdesign.service.menu.MenuSnapshotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜单发布快照服务实现（数据保护核心）。
 * <p>
 * {@link #freeze(Long)} 把 {@code menu_item} 的菜名/分类/单位与 {@code menu_price}、
 * 以及 {@code recipe} 的标准单价一次性写入 {@code menu_snapshot}，写入后不再修改：
 * 已发布菜单的取价结果因此永久稳定，菜品后续改名改价只影响未来菜单，
 * 不影响历史菜单与历史订单。
 * <p>
 * 幂等策略：同一菜单已存在快照时直接返回既有条数（快照表只插不改，不重复插入也不删除）。
 * 事务用普通 {@code @Transactional}（由 {@code MenuService.publish} 发起并加入同一事务，
 * 发布失败整体回滚，不会留下半份快照）。
 */
@Service
@Transactional
public class MenuSnapshotServiceImpl implements MenuSnapshotService
{
	private final MenuRepository menuRepository;

	private final MenuItemRepository menuItemRepository;

	private final RecipeRepository recipeRepository;

	private final MenuSnapshotRepository menuSnapshotRepository;

	public MenuSnapshotServiceImpl(
			MenuRepository menuRepository,
			MenuItemRepository menuItemRepository,
			RecipeRepository recipeRepository,
			MenuSnapshotRepository menuSnapshotRepository) {
		this.menuRepository = menuRepository;
		this.menuItemRepository = menuItemRepository;
		this.recipeRepository = recipeRepository;
		this.menuSnapshotRepository = menuSnapshotRepository;
	}

	@Override
	public int freeze(Long menuId) {
		if (menuId == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "菜单ID不能为空");
		}
		menuRepository.findById(menuId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "菜单不存在，ID：" + menuId));
		long existing = menuSnapshotRepository.countByMenuId(menuId);
		if (existing > 0) {
			return (int) existing;
		}
		List<MenuItem> items = menuItemRepository.findByMenuIdOrderByItemIdAsc(menuId);
		if (items.isEmpty()) {
			throw new BusinessException(ErrorCode.STATE_CONFLICT, "菜单内没有任何菜品，无法冻结快照");
		}
		List<Long> recipeIds = items.stream().map(MenuItem::getRecipeId).toList();
		Map<Long, Recipe> recipes = recipeRepository.findAllById(recipeIds).stream()
				.collect(Collectors.toMap(Recipe::getRecipeId, Function.identity(), (left, right) -> left));
		LocalDateTime frozenAt = LocalDateTime.now();
		for (MenuItem item : items) {
			menuSnapshotRepository.save(toSnapshot(item, recipes.get(item.getRecipeId()), frozenAt));
		}
		return items.size();
	}

	/**
	 * 菜单项 → 不可变快照：标准单价取自菜品当前值，菜单价取自菜单项
	 */
	private MenuSnapshot toSnapshot(MenuItem item, Recipe recipe, LocalDateTime frozenAt) {
		MenuSnapshot snapshot = new MenuSnapshot();
		snapshot.setMenuId(item.getMenuId());
		snapshot.setRecipeId(item.getRecipeId());
		snapshot.setRecipeName(item.getRecipeName());
		snapshot.setCategory(item.getCategory());
		snapshot.setUnit(item.getUnit());
		snapshot.setUnitPrice(recipe == null ? item.getMenuPrice() : recipe.getUnitPrice());
		snapshot.setMenuPrice(item.getMenuPrice());
		snapshot.setFrozenAt(frozenAt);
		return snapshot;
	}
}
