package com.university.webdesign.service.menu;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.domain.menu.Menu;
import com.university.webdesign.domain.menu.MenuItem;
import com.university.webdesign.domain.menu.MenuSnapshot;
import com.university.webdesign.domain.menu.MenuStatus;
import com.university.webdesign.domain.menu.Recipe;
import com.university.webdesign.domain.menu.RecipeStatus;
import com.university.webdesign.event.MenuPublishedEvent;
import com.university.webdesign.repository.menu.MenuItemRepository;
import com.university.webdesign.repository.menu.MenuRepository;
import com.university.webdesign.repository.menu.MenuSnapshotRepository;
import com.university.webdesign.repository.menu.RecipeRepository;
import com.university.webdesign.service.impl.menu.MenuServiceImpl;
import com.university.webdesign.service.menu.dto.MenuCreateCmd;
import com.university.webdesign.service.menu.dto.MenuCreateItem;
import com.university.webdesign.service.menu.dto.MenuQuery;
import com.university.webdesign.service.menu.dto.MenuUpdateCmd;
import com.university.webdesign.service.menu.dto.MenuVO;
import com.university.webdesign.service.menu.dto.PublishResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MenuServiceImpl} 单元测试（Mockito 直接测实现，不启动 Spring 上下文）。
 * <p>
 * 重点覆盖菜单版本链（仅 DRAFT 可改、version 自增）、发布冻结顺序
 * （置 PUBLISHED + 冻结快照 + 发事件）、下架，以及 M2 下单取价入口
 * {@code getCurrent} 的“快照明细 + 菜单价优先、为空回退标准单价”语义。
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceImplTests
{
	@Mock
	private MenuRepository menuRepository;

	@Mock
	private MenuItemRepository menuItemRepository;

	@Mock
	private RecipeRepository recipeRepository;

	@Mock
	private MenuSnapshotRepository menuSnapshotRepository;

	@Mock
	private MenuSnapshotService menuSnapshotService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private MenuServiceImpl menuService;

	@BeforeEach
	void setUp() {
		menuService = new MenuServiceImpl(menuRepository, menuItemRepository, recipeRepository,
				menuSnapshotRepository, menuSnapshotService, eventPublisher);
	}

	@Test
	@DisplayName("创建草稿：菜品信息复制成菜单项副本，菜单价为空时取菜品标准单价")
	void createDraftShouldCopyRecipeAndUseStandardPrice() {
		Recipe recipe = recipe(10L, "小炒肉", new BigDecimal("12.00"));
		recipe.setImageUrl("/uploads/recipe.png");
		when(recipeRepository.findAllById(any())).thenReturn(List.of(recipe));
		when(menuRepository.save(any(Menu.class))).thenAnswer(invocation -> {
			Menu saved = invocation.getArgument(0);
			saved.setMenuId(100L);
			saved.setVersion(0);
			return saved;
		});
		MenuCreateCmd cmd = new MenuCreateCmd();
		cmd.setName("周五午餐");
		cmd.setEffectiveDate(LocalDate.of(2026, 1, 16));
		MenuCreateItem item = new MenuCreateItem();
		item.setRecipeId(10L);
		cmd.setItems(List.of(item));

		Long menuId = menuService.createDraft(cmd);

		assertThat(menuId).isEqualTo(100L);
		verify(menuItemRepository).saveAll(argThat((Iterable<MenuItem> items) -> {
			List<MenuItem> list = new ArrayList<>();
			items.forEach(list::add);
			return list.size() == 1
					&& list.get(0).getMenuId().equals(100L)
					&& list.get(0).getRecipeName().equals("小炒肉")
					&& list.get(0).getCategory().equals("荤菜")
					&& list.get(0).getMenuPrice().compareTo(new BigDecimal("12.00")) == 0
					&& list.get(0).getImageUrl().equals("/uploads/recipe.png");
		}));
	}

	@Test
	@DisplayName("创建草稿：菜品不存在抛 40400")
	void createDraftShouldRejectMissingRecipe() {
		when(recipeRepository.findAllById(any())).thenReturn(List.of());
		MenuCreateCmd cmd = new MenuCreateCmd();
		cmd.setName("周五午餐");
		cmd.setEffectiveDate(LocalDate.of(2026, 1, 16));
		MenuCreateItem item = new MenuCreateItem();
		item.setRecipeId(999L);
		cmd.setItems(List.of(item));

		assertThat(expectBusinessException(() -> menuService.createDraft(cmd)).getErrorCode())
				.isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	@DisplayName("创建草稿：菜品列表为空抛 40001")
	void createDraftShouldRejectEmptyItems() {
		MenuCreateCmd cmd = new MenuCreateCmd();
		cmd.setName("周五午餐");
		cmd.setEffectiveDate(LocalDate.of(2026, 1, 16));

		assertThat(expectBusinessException(() -> menuService.createDraft(cmd)).getErrorCode())
				.isEqualTo(ErrorCode.PARAM_INVALID);
	}

	@Test
	@DisplayName("更新菜单：非草稿抛 40902")
	void updateShouldRejectNonDraftMenu() {
		Menu menu = menu(1L, MenuStatus.PUBLISHED);
		menu.setLocked(true);
		when(menuRepository.findById(1L)).thenReturn(Optional.of(menu));
		MenuUpdateCmd cmd = new MenuUpdateCmd();
		cmd.setName("改个名");

		assertThat(expectBusinessException(() -> menuService.update(1L, cmd)).getErrorCode())
				.isEqualTo(ErrorCode.STATE_CONFLICT);
		verify(menuRepository, never()).saveAndFlush(any(Menu.class));
	}

	@Test
	@DisplayName("更新菜单：version 与库中不一致抛 40902")
	void updateShouldRejectVersionConflict() {
		Menu menu = menu(1L, MenuStatus.DRAFT);
		menu.setVersion(3);
		when(menuRepository.findById(1L)).thenReturn(Optional.of(menu));
		MenuUpdateCmd cmd = new MenuUpdateCmd();
		cmd.setVersion(1);
		cmd.setName("改个名");

		assertThat(expectBusinessException(() -> menuService.update(1L, cmd)).getErrorCode())
				.isEqualTo(ErrorCode.STATE_CONFLICT);
	}

	@Test
	@DisplayName("更新菜单：改动后版本号自增并返回新版本")
	void updateShouldIncreaseVersionAndReturnIt() {
		Menu menu = menu(1L, MenuStatus.DRAFT);
		menu.setVersion(3);
		when(menuRepository.findById(1L)).thenReturn(Optional.of(menu));
		when(menuRepository.saveAndFlush(any(Menu.class))).thenAnswer(invocation -> {
			Menu saved = invocation.getArgument(0);
			saved.setVersion(saved.getVersion() + 1);
			return saved;
		});
		MenuUpdateCmd cmd = new MenuUpdateCmd();
		cmd.setVersion(3);
		cmd.setName("周五午餐（改）");

		int newVersion = menuService.update(1L, cmd);

		assertThat(newVersion).isEqualTo(4);
		assertThat(menu.getName()).isEqualTo("周五午餐（改）");
		assertThat(menu.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("发布菜单：置 PUBLISHED + 锁定 + 冻结快照 + 发 MenuPublishedEvent")
	void publishShouldFreezeSnapshotAndPublishEvent() {
		Menu menu = menu(1L, MenuStatus.DRAFT);
		menu.setVersion(0);
		when(menuRepository.findById(1L)).thenReturn(Optional.of(menu));
		when(menuItemRepository.findByMenuIdOrderByItemIdAsc(1L)).thenReturn(List.of(menuItem(1L, 10L, "小炒肉")));
		when(recipeRepository.findAllById(any())).thenReturn(List.of(recipe(10L, "小炒肉", new BigDecimal("12.00"))));
		when(menuSnapshotService.freeze(1L)).thenReturn(1);
		when(menuRepository.saveAndFlush(any(Menu.class))).thenAnswer(invocation -> {
			Menu saved = invocation.getArgument(0);
			saved.setVersion(1);
			return saved;
		});

		PublishResultVO result = menuService.publish(1L);

		assertThat(result.getMenuId()).isEqualTo(1L);
		assertThat(result.getStatus()).isEqualTo(MenuStatus.PUBLISHED.name());
		assertThat(result.getSnapshotCount()).isEqualTo(1);
		assertThat(result.getVersion()).isEqualTo(1);
		assertThat(result.getPublishedAt()).isNotNull();
		assertThat(menu.isLocked()).isTrue();
		assertThat(menu.getStatus()).isEqualTo(MenuStatus.PUBLISHED);
		ArgumentCaptor<MenuPublishedEvent> captor = ArgumentCaptor.forClass(MenuPublishedEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().menuId()).isEqualTo(1L);
		assertThat(captor.getValue().snapshotCount()).isEqualTo(1);
		assertThat(captor.getValue().effectiveDate()).isEqualTo(menu.getEffectiveDate());
	}

	@Test
	@DisplayName("发布菜单：菜单为空抛 40902")
	void publishShouldRejectEmptyMenu() {
		when(menuRepository.findById(1L)).thenReturn(Optional.of(menu(1L, MenuStatus.DRAFT)));
		when(menuItemRepository.findByMenuIdOrderByItemIdAsc(1L)).thenReturn(List.of());

		assertThat(expectBusinessException(() -> menuService.publish(1L)).getErrorCode())
				.isEqualTo(ErrorCode.STATE_CONFLICT);
		verify(menuSnapshotService, never()).freeze(any());
	}

	@Test
	@DisplayName("发布菜单：含已下架菜品抛 40902")
	void publishShouldRejectDisabledRecipe() {
		when(menuRepository.findById(1L)).thenReturn(Optional.of(menu(1L, MenuStatus.DRAFT)));
		when(menuItemRepository.findByMenuIdOrderByItemIdAsc(1L)).thenReturn(List.of(menuItem(1L, 10L, "小炒肉")));
		Recipe disabled = recipe(10L, "小炒肉", new BigDecimal("12.00"));
		disabled.setStatus(RecipeStatus.DISABLED);
		when(recipeRepository.findAllById(any())).thenReturn(List.of(disabled));

		assertThat(expectBusinessException(() -> menuService.publish(1L)).getErrorCode())
				.isEqualTo(ErrorCode.STATE_CONFLICT);
		verify(menuSnapshotService, never()).freeze(any());
	}

	@Test
	@DisplayName("下架菜单：非已发布状态抛 40902")
	void unpublishShouldRejectDraftMenu() {
		when(menuRepository.findById(1L)).thenReturn(Optional.of(menu(1L, MenuStatus.DRAFT)));

		assertThat(expectBusinessException(() -> menuService.unpublish(1L, "临时停用")).getErrorCode())
				.isEqualTo(ErrorCode.STATE_CONFLICT);
	}

	@Test
	@DisplayName("下架菜单：置 OFFLINE 并记录下架时间与原因")
	void unpublishShouldMarkOffline() {
		Menu menu = menu(1L, MenuStatus.PUBLISHED);
		menu.setLocked(true);
		when(menuRepository.findById(1L)).thenReturn(Optional.of(menu));

		menuService.unpublish(1L, "原料短缺");

		assertThat(menu.getStatus()).isEqualTo(MenuStatus.OFFLINE);
		assertThat(menu.getOfflineAt()).isNotNull();
		assertThat(menu.getOfflineReason()).isEqualTo("原料短缺");
		assertThat(menu.isLocked()).isTrue();
	}

	@Test
	@DisplayName("取当日菜单：返回快照明细，menuPrice 为菜单价")
	void getCurrentShouldReturnSnapshotPrice() {
		LocalDate date = LocalDate.of(2026, 1, 16);
		Menu menu = menu(1L, MenuStatus.PUBLISHED);
		menu.setLocked(true);
		when(menuRepository.findByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDescMenuIdDesc(
				MenuStatus.PUBLISHED, date)).thenReturn(List.of(menu));
		when(menuSnapshotRepository.findByMenuIdOrderBySnapshotIdAsc(1L))
				.thenReturn(List.of(snapshot(1L, 10L, new BigDecimal("12.00"), new BigDecimal("10.00"))));
		when(menuItemRepository.findByMenuIdOrderByItemIdAsc(1L)).thenReturn(List.of(menuItem(1L, 10L, "小炒肉")));

		MenuVO vo = menuService.getCurrent(date);

		assertThat(vo).isNotNull();
		assertThat(vo.getStatus()).isEqualTo(MenuStatus.PUBLISHED.name());
		assertThat(vo.getItems()).hasSize(1);
		assertThat(vo.getItems().get(0).getRecipeId()).isEqualTo(10L);
		assertThat(vo.getItems().get(0).getItemId()).isEqualTo(10L);
		assertThat(vo.getItems().get(0).getUnitPrice()).isEqualByComparingTo("12.00");
		assertThat(vo.getItems().get(0).getMenuPrice()).isEqualByComparingTo("10.00");
		assertThat(vo.getItemsByCategory()).containsKey("荤菜");
	}

	@Test
	@DisplayName("取当日菜单：菜单价为空时下单取价回退标准单价")
	void getCurrentShouldFallbackToUnitPrice() {
		LocalDate date = LocalDate.of(2026, 1, 16);
		Menu menu = menu(1L, MenuStatus.PUBLISHED);
		when(menuRepository.findByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDescMenuIdDesc(
				MenuStatus.PUBLISHED, date)).thenReturn(List.of(menu));
		when(menuSnapshotRepository.findByMenuIdOrderBySnapshotIdAsc(1L))
				.thenReturn(List.of(snapshot(1L, 10L, new BigDecimal("12.00"), null)));
		when(menuItemRepository.findByMenuIdOrderByItemIdAsc(1L)).thenReturn(List.of());

		MenuVO vo = menuService.getCurrent(date);

		assertThat(vo.getItems().get(0).getMenuPrice()).isEqualByComparingTo("12.00");
	}

	@Test
	@DisplayName("取当日菜单：没有已发布菜单时返回 null")
	void getCurrentShouldReturnNullWhenNoPublishedMenu() {
		LocalDate date = LocalDate.of(2026, 1, 16);
		when(menuRepository.findByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDescMenuIdDesc(
				MenuStatus.PUBLISHED, date)).thenReturn(List.of());

		assertThat(menuService.getCurrent(date)).isNull();
	}

	@Test
	@DisplayName("历史菜单分页：withItems=true 时已发布菜单带冻结快照明细")
	void pageShouldLoadSnapshotItemsWhenWithItems() {
		Menu menu = menu(1L, MenuStatus.PUBLISHED);
		when(menuRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(menu)));
		when(menuItemRepository.findByMenuIdInOrderByMenuIdAscItemIdAsc(anyList()))
				.thenReturn(List.of(menuItem(1L, 10L, "小炒肉")));
		when(menuSnapshotRepository.findByMenuIdInOrderByMenuIdAscSnapshotIdAsc(anyList()))
				.thenReturn(List.of(snapshot(1L, 10L, new BigDecimal("12.00"), new BigDecimal("9.90"))));
		MenuQuery query = new MenuQuery();
		query.setWithItems(Boolean.TRUE);

		PageResult<MenuVO> page = menuService.page(query);

		assertThat(page.getTotal()).isEqualTo(1);
		assertThat(page.getList().get(0).getItems()).hasSize(1);
		assertThat(page.getList().get(0).getItems().get(0).getMenuPrice()).isEqualByComparingTo("9.90");
	}

	@Test
	@DisplayName("历史菜单分页：withItems=false 时不查询明细")
	void pageShouldSkipItemsByDefault() {
		Menu menu = menu(1L, MenuStatus.PUBLISHED);
		when(menuRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(menu)));

		PageResult<MenuVO> page = menuService.page(new MenuQuery());

		assertThat(page.getList().get(0).getItems()).isEmpty();
		verify(menuItemRepository, never()).findByMenuIdInOrderByMenuIdAscItemIdAsc(anyList());
	}

	private static Menu menu(Long menuId, MenuStatus status) {
		Menu menu = new Menu();
		menu.setMenuId(menuId);
		menu.setName("周五午餐");
		menu.setStatus(status);
		menu.setEffectiveDate(LocalDate.of(2026, 1, 16));
		menu.setVersion(0);
		menu.setCreatedAt(LocalDateTime.now());
		menu.setUpdatedAt(LocalDateTime.now());
		return menu;
	}

	private static MenuItem menuItem(Long menuId, Long recipeId, String recipeName) {
		MenuItem item = new MenuItem();
		item.setItemId(1L);
		item.setMenuId(menuId);
		item.setRecipeId(recipeId);
		item.setRecipeName(recipeName);
		item.setCategory("荤菜");
		item.setUnit("份");
		item.setImageUrl("/uploads/recipe.png");
		item.setMenuPrice(new BigDecimal("12.00"));
		return item;
	}

	private static MenuSnapshot snapshot(Long menuId, Long recipeId, BigDecimal unitPrice, BigDecimal menuPrice) {
		MenuSnapshot snapshot = new MenuSnapshot();
		snapshot.setSnapshotId(1L);
		snapshot.setMenuId(menuId);
		snapshot.setRecipeId(recipeId);
		snapshot.setRecipeName("小炒肉");
		snapshot.setCategory("荤菜");
		snapshot.setUnit("份");
		snapshot.setUnitPrice(unitPrice);
		snapshot.setMenuPrice(menuPrice);
		snapshot.setFrozenAt(LocalDateTime.now());
		return snapshot;
	}

	private static Recipe recipe(Long recipeId, String name, BigDecimal unitPrice) {
		Recipe recipe = new Recipe();
		recipe.setRecipeId(recipeId);
		recipe.setRecipeName(name);
		recipe.setCategory("荤菜");
		recipe.setUnit("份");
		recipe.setUnitPrice(unitPrice);
		recipe.setStatus(RecipeStatus.ACTIVE);
		recipe.setVersion(0);
		return recipe;
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
