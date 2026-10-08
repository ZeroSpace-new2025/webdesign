package com.university.webdesign.service.menu;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.domain.menu.Menu;
import com.university.webdesign.domain.menu.MenuItem;
import com.university.webdesign.domain.menu.MenuSnapshot;
import com.university.webdesign.domain.menu.Recipe;
import com.university.webdesign.repository.menu.MenuItemRepository;
import com.university.webdesign.repository.menu.MenuRepository;
import com.university.webdesign.repository.menu.MenuSnapshotRepository;
import com.university.webdesign.repository.menu.RecipeRepository;
import com.university.webdesign.service.impl.menu.MenuSnapshotServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MenuSnapshotServiceImpl} 单元测试。
 * <p>
 * 覆盖“数据保护核心”的快照冻结：菜名/分类/单位/单价/菜单价完整落表、
 * 只插不改（已冻结时幂等返回既有条数）、空菜单与不存在菜单的错误码。
 */
@ExtendWith(MockitoExtension.class)
class MenuSnapshotServiceImplTests
{
	@Mock
	private MenuRepository menuRepository;

	@Mock
	private MenuItemRepository menuItemRepository;

	@Mock
	private RecipeRepository recipeRepository;

	@Mock
	private MenuSnapshotRepository menuSnapshotRepository;

	private MenuSnapshotServiceImpl menuSnapshotService;

	@BeforeEach
	void setUp() {
		menuSnapshotService = new MenuSnapshotServiceImpl(menuRepository, menuItemRepository, recipeRepository,
				menuSnapshotRepository);
	}

	@Test
	@DisplayName("冻结快照：菜名/分类/单位取自菜单项，标准单价取自菜品，菜单价取自菜单项")
	void freezeShouldInsertSnapshotRows() {
		when(menuRepository.findById(1L)).thenReturn(Optional.of(new Menu()));
		when(menuSnapshotRepository.countByMenuId(1L)).thenReturn(0L);
		when(menuItemRepository.findByMenuIdOrderByItemIdAsc(1L))
				.thenReturn(List.of(menuItem(1L, 10L, "小炒肉", new BigDecimal("12.00"))));
		Recipe recipe = new Recipe();
		recipe.setRecipeId(10L);
		recipe.setUnitPrice(new BigDecimal("15.00"));
		when(recipeRepository.findAllById(any())).thenReturn(List.of(recipe));

		int count = menuSnapshotService.freeze(1L);

		assertThat(count).isEqualTo(1);
		ArgumentCaptor<MenuSnapshot> captor = ArgumentCaptor.forClass(MenuSnapshot.class);
		verify(menuSnapshotRepository).save(captor.capture());
		MenuSnapshot saved = captor.getValue();
		assertThat(saved.getMenuId()).isEqualTo(1L);
		assertThat(saved.getRecipeId()).isEqualTo(10L);
		assertThat(saved.getRecipeName()).isEqualTo("小炒肉");
		assertThat(saved.getCategory()).isEqualTo("荤菜");
		assertThat(saved.getUnit()).isEqualTo("份");
		assertThat(saved.getUnitPrice()).isEqualByComparingTo("15.00");
		assertThat(saved.getMenuPrice()).isEqualByComparingTo("12.00");
		assertThat(saved.getFrozenAt()).isNotNull();
	}

	@Test
	@DisplayName("冻结快照：已存在快照时幂等返回既有条数，不重复插入")
	void freezeShouldBeIdempotent() {
		when(menuRepository.findById(1L)).thenReturn(Optional.of(new Menu()));
		when(menuSnapshotRepository.countByMenuId(1L)).thenReturn(3L);

		int count = menuSnapshotService.freeze(1L);

		assertThat(count).isEqualTo(3);
		verify(menuSnapshotRepository, never()).save(any(MenuSnapshot.class));
		verify(menuItemRepository, never()).findByMenuIdOrderByItemIdAsc(any());
	}

	@Test
	@DisplayName("冻结快照：菜单内没有菜品抛 40902")
	void freezeShouldRejectEmptyMenu() {
		when(menuRepository.findById(1L)).thenReturn(Optional.of(new Menu()));
		when(menuSnapshotRepository.countByMenuId(1L)).thenReturn(0L);
		when(menuItemRepository.findByMenuIdOrderByItemIdAsc(1L)).thenReturn(List.of());

		assertThat(expectBusinessException(() -> menuSnapshotService.freeze(1L)).getErrorCode())
				.isEqualTo(ErrorCode.STATE_CONFLICT);
	}

	@Test
	@DisplayName("冻结快照：菜单不存在抛 40400")
	void freezeShouldRejectMissingMenu() {
		when(menuRepository.findById(9L)).thenReturn(Optional.empty());

		assertThat(expectBusinessException(() -> menuSnapshotService.freeze(9L)).getErrorCode())
				.isEqualTo(ErrorCode.NOT_FOUND);
	}

	private static MenuItem menuItem(Long menuId, Long recipeId, String recipeName, BigDecimal menuPrice) {
		MenuItem item = new MenuItem();
		item.setItemId(1L);
		item.setMenuId(menuId);
		item.setRecipeId(recipeId);
		item.setRecipeName(recipeName);
		item.setCategory("荤菜");
		item.setUnit("份");
		item.setMenuPrice(menuPrice);
		return item;
	}

	private static BusinessException expectBusinessException(Runnable action) {
		try {
			action.run();
		} catch (BusinessException e) {
			return e;
		} catch (RuntimeException e) {
			throw new AssertionError("期望 BusinessException，实际抛出：" + e, e);
		}
		throw new AssertionError("期望抛出 BusinessException，但没有抛出异常");
	}
}
