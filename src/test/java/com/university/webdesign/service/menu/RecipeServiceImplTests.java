package com.university.webdesign.service.menu;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.domain.menu.Recipe;
import com.university.webdesign.domain.menu.RecipeStatus;
import com.university.webdesign.event.RecipeChangedEvent;
import com.university.webdesign.repository.menu.MenuItemRepository;
import com.university.webdesign.repository.menu.RecipeRepository;
import com.university.webdesign.service.impl.menu.ImageStorageService;
import com.university.webdesign.service.impl.menu.RecipeServiceImpl;
import com.university.webdesign.service.menu.dto.CategoryVO;
import com.university.webdesign.service.menu.dto.RecipeCreateCmd;
import com.university.webdesign.service.menu.dto.RecipeUpdateCmd;
import com.university.webdesign.service.menu.dto.RecipeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RecipeServiceImpl} 单元测试（Mockito 直接测实现，不启动 Spring 上下文）。
 * <p>
 * 覆盖 M1 的硬性错误码：重名 40901、不存在 40400、乐观锁冲突 40902、
 * 被已发布菜单引用时下架 42203、参数非法 40001，以及分类聚合与事件发布。
 */
@ExtendWith(MockitoExtension.class)
class RecipeServiceImplTests
{
	@Mock
	private RecipeRepository recipeRepository;

	@Mock
	private MenuItemRepository menuItemRepository;

	@Mock
	private ImageStorageService imageStorageService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private RecipeServiceImpl recipeService;

	@BeforeEach
	void setUp() {
		recipeService = new RecipeServiceImpl(recipeRepository, menuItemRepository, imageStorageService, eventPublisher);
	}

	@Test
	@DisplayName("新增菜品：同名抛 40901")
	void createShouldRejectDuplicatedName() {
		RecipeCreateCmd cmd = createCmd("小炒肉", new BigDecimal("12.00"));
		when(recipeRepository.existsByRecipeName("小炒肉")).thenReturn(true);

		assertThat(expectBusinessException(() -> recipeService.create(cmd)).getErrorCode())
				.isEqualTo(ErrorCode.DUPLICATED);
		verify(recipeRepository, never()).save(any(Recipe.class));
	}

	@Test
	@DisplayName("新增菜品：单价为空抛 40001")
	void createShouldRejectMissingPrice() {
		RecipeCreateCmd cmd = createCmd("小炒肉", null);

		assertThat(expectBusinessException(() -> recipeService.create(cmd)).getErrorCode())
				.isEqualTo(ErrorCode.PARAM_INVALID);
	}

	@Test
	@DisplayName("新增菜品：落库并发布 RecipeChangedEvent(CREATED)")
	void createShouldSaveAndPublishEvent() {
		RecipeCreateCmd cmd = createCmd("小炒肉", new BigDecimal("12.00"));
		cmd.setCategory("荤菜");
		cmd.setUnit("份");
		when(recipeRepository.existsByRecipeName("小炒肉")).thenReturn(false);
		when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
			Recipe saved = invocation.getArgument(0);
			saved.setRecipeId(5L);
			saved.setVersion(0);
			return saved;
		});

		Long recipeId = recipeService.create(cmd);

		assertThat(recipeId).isEqualTo(5L);
		ArgumentCaptor<RecipeChangedEvent> captor = ArgumentCaptor.forClass(RecipeChangedEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().recipeId()).isEqualTo(5L);
		assertThat(captor.getValue().changeType()).isEqualTo(RecipeChangedEvent.CREATED);
	}

	@Test
	@DisplayName("更新菜品：version 与库中不一致抛 40902")
	void updateShouldRejectVersionConflict() {
		Recipe recipe = recipe(1L, "小炒肉", RecipeStatus.ACTIVE, new BigDecimal("12.00"));
		recipe.setVersion(3);
		when(recipeRepository.findById(1L)).thenReturn(java.util.Optional.of(recipe));
		RecipeUpdateCmd cmd = new RecipeUpdateCmd();
		cmd.setVersion(1);
		cmd.setUnitPrice(new BigDecimal("13.00"));

		assertThat(expectBusinessException(() -> recipeService.update(1L, cmd)).getErrorCode())
				.isEqualTo(ErrorCode.STATE_CONFLICT);
		verify(recipeRepository, never()).saveAndFlush(any(Recipe.class));
	}

	@Test
	@DisplayName("更新菜品：改名撞车抛 40901")
	void updateShouldRejectDuplicatedName() {
		Recipe recipe = recipe(1L, "小炒肉", RecipeStatus.ACTIVE, new BigDecimal("12.00"));
		when(recipeRepository.findById(1L)).thenReturn(java.util.Optional.of(recipe));
		when(recipeRepository.existsByRecipeNameAndRecipeIdNot("红烧肉", 1L)).thenReturn(true);
		RecipeUpdateCmd cmd = new RecipeUpdateCmd();
		cmd.setName("红烧肉");

		assertThat(expectBusinessException(() -> recipeService.update(1L, cmd)).getErrorCode())
				.isEqualTo(ErrorCode.DUPLICATED);
	}

	@Test
	@DisplayName("更新菜品：成功时只改本体并发布 RecipeChangedEvent(UPDATED)")
	void updateShouldApplyFieldsAndPublishEvent() {
		Recipe recipe = recipe(1L, "小炒肉", RecipeStatus.ACTIVE, new BigDecimal("12.00"));
		when(recipeRepository.findById(1L)).thenReturn(java.util.Optional.of(recipe));
		RecipeUpdateCmd cmd = new RecipeUpdateCmd();
		cmd.setUnitPrice(new BigDecimal("13.50"));
		cmd.setUnit("份");

		recipeService.update(1L, cmd);

		assertThat(recipe.getUnitPrice()).isEqualByComparingTo("13.50");
		assertThat(recipe.getUnit()).isEqualTo("份");
		ArgumentCaptor<RecipeChangedEvent> captor = ArgumentCaptor.forClass(RecipeChangedEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().changeType()).isEqualTo(RecipeChangedEvent.UPDATED);
	}

	@Test
	@DisplayName("下架菜品：被已发布菜单引用抛 42203")
	void disableShouldRejectWhenReferencedByPublishedMenu() {
		Recipe recipe = recipe(1L, "小炒肉", RecipeStatus.ACTIVE, new BigDecimal("12.00"));
		when(recipeRepository.findById(1L)).thenReturn(java.util.Optional.of(recipe));
		when(menuItemRepository.countMenusReferencing(1L, com.university.webdesign.domain.menu.MenuStatus.PUBLISHED))
				.thenReturn(2L);

		assertThat(expectBusinessException(() -> recipeService.disable(1L, "停售")).getErrorCode())
				.isEqualTo(ErrorCode.DATA_PROTECTED);
		assertThat(recipe.getStatus()).isEqualTo(RecipeStatus.ACTIVE);
	}

	@Test
	@DisplayName("下架菜品：逻辑下架并返回仍可编辑的草稿菜单数")
	void disableShouldMarkDisabledAndReturnDraftMenuCount() {
		Recipe recipe = recipe(1L, "小炒肉", RecipeStatus.ACTIVE, new BigDecimal("12.00"));
		when(recipeRepository.findById(1L)).thenReturn(java.util.Optional.of(recipe));
		when(menuItemRepository.countMenusReferencing(1L, com.university.webdesign.domain.menu.MenuStatus.PUBLISHED))
				.thenReturn(0L);
		when(menuItemRepository.countMenusReferencing(1L, com.university.webdesign.domain.menu.MenuStatus.DRAFT))
				.thenReturn(2L);

		int affected = recipeService.disable(1L, "停售");

		assertThat(affected).isEqualTo(2);
		assertThat(recipe.getStatus()).isEqualTo(RecipeStatus.DISABLED);
		assertThat(recipe.getOfflineReason()).isEqualTo("停售");
		verify(recipeRepository).saveAndFlush(recipe);
	}

	@Test
	@DisplayName("菜品详情：不存在抛 40400")
	void getByIdShouldRejectMissingRecipe() {
		when(recipeRepository.findById(99L)).thenReturn(java.util.Optional.empty());

		assertThat(expectBusinessException(() -> recipeService.getById(99L)).getErrorCode())
				.isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	@DisplayName("菜品详情：字段映射完整（含派生分类ID）")
	void getByIdShouldMapFields() {
		Recipe recipe = recipe(7L, "米饭", RecipeStatus.ACTIVE, new BigDecimal("1.00"));
		recipe.setCategory("主食");
		recipe.setVersion(2);
		when(recipeRepository.findById(7L)).thenReturn(java.util.Optional.of(recipe));

		RecipeVO vo = recipeService.getById(7L);

		assertThat(vo.getRecipeId()).isEqualTo(7L);
		assertThat(vo.getName()).isEqualTo("米饭");
		assertThat(vo.getStatus()).isEqualTo("ACTIVE");
		assertThat(vo.getStatusText()).isEqualTo("可售");
		assertThat(vo.getCategoryId()).isEqualTo((long) Math.abs("主食".hashCode()));
	}

	@Test
	@DisplayName("分类字典：按 recipe.category 去重聚合并统计菜品数")
	void listCategoriesShouldAggregateFromRecipeCategory() {
		when(recipeRepository.findDistinctCategories()).thenReturn(List.of("主食", "荤菜"));
		when(recipeRepository.countByCategory("主食")).thenReturn(1L);
		when(recipeRepository.countByCategory("荤菜")).thenReturn(3L);

		List<CategoryVO> categories = recipeService.listCategories();

		assertThat(categories).hasSize(2);
		assertThat(categories.get(0).getCategoryName()).isEqualTo("主食");
		assertThat(categories.get(0).getCategoryId()).isEqualTo((long) Math.abs("主食".hashCode()));
		assertThat(categories.get(0).getRecipeCount()).isEqualTo(1L);
		assertThat(categories.get(1).getRecipeCount()).isEqualTo(3L);
	}

	@Test
	@DisplayName("上传图片：委托给本地存储服务并返回可访问地址")
	void uploadImageShouldDelegateToStorage() {
		when(imageStorageService.store(any())).thenReturn("/uploads/abc.png");

		assertThat(recipeService.uploadImage(null)).isEqualTo("/uploads/abc.png");
		verify(imageStorageService).store(any());
	}

	private static RecipeCreateCmd createCmd(String name, BigDecimal unitPrice) {
		RecipeCreateCmd cmd = new RecipeCreateCmd();
		cmd.setName(name);
		cmd.setUnitPrice(unitPrice);
		return cmd;
	}

	private static Recipe recipe(Long recipeId, String name, RecipeStatus status, BigDecimal unitPrice) {
		Recipe recipe = new Recipe();
		recipe.setRecipeId(recipeId);
		recipe.setRecipeName(name);
		recipe.setStatus(status);
		recipe.setUnitPrice(unitPrice);
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
