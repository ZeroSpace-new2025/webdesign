package com.university.webdesign.service.impl.menu;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.UserContextHolder;
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
import com.university.webdesign.service.menu.MenuService;
import com.university.webdesign.service.menu.MenuSnapshotService;
import com.university.webdesign.service.menu.dto.MenuCreateCmd;
import com.university.webdesign.service.menu.dto.MenuCreateItem;
import com.university.webdesign.service.menu.dto.MenuItemVO;
import com.university.webdesign.service.menu.dto.MenuQuery;
import com.university.webdesign.service.menu.dto.MenuUpdateCmd;
import com.university.webdesign.service.menu.dto.MenuUpdateItem;
import com.university.webdesign.service.menu.dto.MenuVO;
import com.university.webdesign.service.menu.dto.PublishResultVO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜单服务实现（M1-08 ~ M1-13）。
 * <p>
 * 核心不变量与执行顺序：
 * <ol>
 *     <li><b>版本链</b>：仅 {@code DRAFT} 可改，每次改动 {@code version} 自增并返回新版本号；
 *         {@code PUBLISHED} 后 {@code locked=true}，不可改不可删，只能下架；</li>
 *     <li><b>发布冻结</b>：{@code publish} 依次执行“校验菜单非空且菜品有效 → 置 PUBLISHED +
 *         locked + publishedAt → MenuSnapshotService.freeze → 发布 MenuPublishedEvent”；</li>
 *     <li><b>取价入口</b>：{@link #getCurrent(LocalDate)} 返回当日已发布且已冻结快照的菜单，
 *         {@code items[].menuPrice} 为下单取价字段（菜单价优先，为空回退标准单价）。</li>
 * </ol>
 * 事务统一普通 {@code @Transactional}，查询也不加 {@code readOnly = true}
 * （避免只读事务把 flush 模式切成 MANUAL，导致“先改后查”读到旧数据）。
 */
@Service
@Transactional
public class MenuServiceImpl implements MenuService
{
	/**
	 * 明细中没有分类时的分组名
	 */
	private static final String UNCATEGORIZED = "未分类";

	private final MenuRepository menuRepository;

	private final MenuItemRepository menuItemRepository;

	private final RecipeRepository recipeRepository;

	private final MenuSnapshotRepository menuSnapshotRepository;

	private final MenuSnapshotService menuSnapshotService;

	private final ApplicationEventPublisher eventPublisher;

	public MenuServiceImpl(
			MenuRepository menuRepository,
			MenuItemRepository menuItemRepository,
			RecipeRepository recipeRepository,
			MenuSnapshotRepository menuSnapshotRepository,
			MenuSnapshotService menuSnapshotService,
			ApplicationEventPublisher eventPublisher) {
		this.menuRepository = menuRepository;
		this.menuItemRepository = menuItemRepository;
		this.recipeRepository = recipeRepository;
		this.menuSnapshotRepository = menuSnapshotRepository;
		this.menuSnapshotService = menuSnapshotService;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public Long createDraft(MenuCreateCmd cmd) {
		if (cmd == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "请求体不能为空");
		}
		String name = requireName(cmd.getName());
		LocalDate effectiveDate = requireEffectiveDate(cmd.getEffectiveDate());
		List<ItemRequest> requests = new ArrayList<>();
		if (cmd.getItems() != null) {
			for (MenuCreateItem item : cmd.getItems()) {
				if (item == null) {
					continue;
				}
				requests.add(new ItemRequest(item.getRecipeId(), item.getMenuPrice()));
			}
		}
		List<MenuItem> items = buildMenuItems(requests);
		LocalDateTime now = LocalDateTime.now();
		Menu menu = new Menu();
		menu.setName(name);
		menu.setDescription(normalize(cmd.getDescription()));
		menu.setEffectiveDate(effectiveDate);
		menu.setStatus(MenuStatus.DRAFT);
		menu.setLocked(false);
		menu.setCreatedBy(UserContextHolder.currentUserId());
		menu.setCreatedAt(now);
		menu.setUpdatedAt(now);
		Menu saved = menuRepository.save(menu);
		saveMenuItems(saved.getMenuId(), items);
		return saved.getMenuId();
	}

	@Override
	public int update(Long menuId, MenuUpdateCmd cmd) {
		if (cmd == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "请求体不能为空");
		}
		Menu menu = requireMenu(menuId);
		if (menu.getStatus() != MenuStatus.DRAFT || menu.isLocked()) {
			throw new BusinessException(ErrorCode.STATE_CONFLICT,
					"菜单已发布或已下架，不可修改；请复制为新草稿后再调整（当前状态："
							+ label(menu.getStatus()) + "）");
		}
		checkVersion(menu.getVersion(), cmd.getVersion());
		if (cmd.getName() != null) {
			menu.setName(requireName(cmd.getName()));
		}
		if (cmd.getDescription() != null) {
			menu.setDescription(normalize(cmd.getDescription()));
		}
		if (cmd.getEffectiveDate() != null) {
			menu.setEffectiveDate(cmd.getEffectiveDate());
		}
		if (cmd.getItems() != null) {
			List<ItemRequest> requests = new ArrayList<>();
			for (MenuUpdateItem item : cmd.getItems()) {
				if (item == null) {
					continue;
				}
				requests.add(new ItemRequest(item.getRecipeId(), item.getMenuPrice()));
			}
			List<MenuItem> items = buildMenuItems(requests);
			menuItemRepository.deleteAllByMenuId(menuId);
			saveMenuItems(menuId, items);
		}
		// 只要改动过就一定刷新 updated_at，从而保证 @Version 递增（版本链每次改动自增）
		menu.setUpdatedAt(LocalDateTime.now());
		menuRepository.saveAndFlush(menu);
		return menu.getVersion() == null ? 0 : menu.getVersion();
	}

	@Override
	public PublishResultVO publish(Long menuId) {
		Menu menu = requireMenu(menuId);
		if (menu.getStatus() != MenuStatus.DRAFT) {
			throw new BusinessException(ErrorCode.STATE_CONFLICT,
					"仅草稿菜单可发布（当前状态：" + label(menu.getStatus()) + "）");
		}
		List<MenuItem> items = menuItemRepository.findByMenuIdOrderByItemIdAsc(menuId);
		if (items.isEmpty()) {
			throw new BusinessException(ErrorCode.STATE_CONFLICT, "菜单内没有任何菜品，无法发布");
		}
		validatePublishable(items);
		LocalDateTime publishedAt = LocalDateTime.now();
		menu.setStatus(MenuStatus.PUBLISHED);
		menu.setLocked(true);
		menu.setPublishedAt(publishedAt);
		menu.setUpdatedAt(publishedAt);
		menuRepository.saveAndFlush(menu);
		// 数据保护核心：冻结菜名/分类/单位/单价/菜单价，只插不改
		int snapshotCount = menuSnapshotService.freeze(menuId);
		// 事务提交后由订阅方（M2 刷新当日菜单缓存等）处理
		eventPublisher.publishEvent(new MenuPublishedEvent(menu.getMenuId(), menu.getVersion(),
				menu.getEffectiveDate(), snapshotCount));
		PublishResultVO result = new PublishResultVO();
		result.setMenuId(menu.getMenuId());
		result.setStatus(menu.getStatus().name());
		result.setPublishedAt(menu.getPublishedAt());
		result.setSnapshotCount(snapshotCount);
		result.setVersion(menu.getVersion());
		return result;
	}

	@Override
	public void unpublish(Long menuId, String reason) {
		Menu menu = requireMenu(menuId);
		if (menu.getStatus() != MenuStatus.PUBLISHED) {
			throw new BusinessException(ErrorCode.STATE_CONFLICT,
					"仅已发布菜单可下架（当前状态：" + label(menu.getStatus()) + "）");
		}
		LocalDateTime offlineAt = LocalDateTime.now();
		menu.setStatus(MenuStatus.OFFLINE);
		menu.setOfflineAt(offlineAt);
		menu.setOfflineReason(truncate(normalize(reason), 200));
		// 保持 locked=true：一旦发布过就是历史菜单，只能查阅不能再改
		menu.setUpdatedAt(offlineAt);
		menuRepository.saveAndFlush(menu);
	}

	@Override
	public MenuVO getCurrent(LocalDate date) {
		LocalDate target = date == null ? LocalDate.now() : date;
		List<Menu> candidates = menuRepository
				.findByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDescMenuIdDesc(
						MenuStatus.PUBLISHED, target);
		for (Menu menu : candidates) {
			List<MenuSnapshot> snapshots = menuSnapshotRepository.findByMenuIdOrderBySnapshotIdAsc(menu.getMenuId());
			if (snapshots.isEmpty()) {
				// 已发布但未冻结快照的菜单不可用于下单取价，继续找上一份生效菜单
				continue;
			}
			List<MenuItem> items = menuItemRepository.findByMenuIdOrderByItemIdAsc(menu.getMenuId());
			return toVO(menu, toItemVOsFromSnapshots(snapshots, imageUrls(items)));
		}
		return null;
	}

	@Override
	public PageResult<MenuVO> page(MenuQuery q) {
		MenuQuery query = q == null ? new MenuQuery() : q;
		Pageable pageable = PageRequest.of(query.toSpringPageNumber(), query.normalizedPageSize(),
				Sort.by(Sort.Direction.DESC, "effectiveDate").and(Sort.by(Sort.Direction.DESC, "menuId")));
		Page<Menu> page = menuRepository.findAll(buildSpecification(query), pageable);
		boolean withItems = Boolean.TRUE.equals(query.getWithItems());
		Map<Long, List<MenuItemVO>> itemsByMenu = withItems ? loadItemVOs(page.getContent()) : Map.of();
		List<MenuVO> list = page.getContent().stream()
				.map(menu -> toVO(menu, itemsByMenu.getOrDefault(menu.getMenuId(), List.of())))
				.toList();
		return new PageResult<>(page.getTotalElements(), list);
	}

	/**
	 * 批量装配历史菜单明细：已发布/已下架菜单取冻结快照，草稿菜单取当前菜单项
	 */
	private Map<Long, List<MenuItemVO>> loadItemVOs(List<Menu> menus) {
		List<Long> menuIds = menus.stream().map(Menu::getMenuId).filter(id -> id != null).toList();
		if (menuIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, List<MenuItem>> itemsByMenu = menuItemRepository
				.findByMenuIdInOrderByMenuIdAscItemIdAsc(menuIds).stream()
				.collect(Collectors.groupingBy(MenuItem::getMenuId, LinkedHashMap::new, Collectors.toList()));
		Map<Long, List<MenuSnapshot>> snapshotsByMenu = menuSnapshotRepository
				.findByMenuIdInOrderByMenuIdAscSnapshotIdAsc(menuIds).stream()
				.collect(Collectors.groupingBy(MenuSnapshot::getMenuId, LinkedHashMap::new, Collectors.toList()));
		Map<Long, Recipe> recipes = null;
		Map<Long, List<MenuItemVO>> result = new LinkedHashMap<>();
		for (Menu menu : menus) {
			Long menuId = menu.getMenuId();
			List<MenuSnapshot> snapshots = snapshotsByMenu.getOrDefault(menuId, List.of());
			List<MenuItem> items = itemsByMenu.getOrDefault(menuId, List.of());
			if (snapshots.isEmpty()) {
				// 草稿菜单没有快照，才需要回查菜品表取标准单价（懒加载，避免无谓查询）
				if (recipes == null) {
					recipes = loadRecipes(itemsByMenu);
				}
				result.put(menuId, toItemVOsFromMenuItems(items, recipes));
			} else {
				result.put(menuId, toItemVOsFromSnapshots(snapshots, imageUrls(items)));
			}
		}
		return result;
	}

	/**
	 * 草稿菜单取当前菜品标准单价（已发布菜单取快照价，不查菜品表）
	 */
	private Map<Long, Recipe> loadRecipes(Map<Long, List<MenuItem>> itemsByMenu) {
		Set<Long> recipeIds = new HashSet<>();
		for (List<MenuItem> items : itemsByMenu.values()) {
			for (MenuItem item : items) {
				if (item.getRecipeId() != null) {
					recipeIds.add(item.getRecipeId());
				}
			}
		}
		if (recipeIds.isEmpty()) {
			return Map.of();
		}
		return recipeRepository.findAllById(recipeIds).stream()
				.collect(Collectors.toMap(Recipe::getRecipeId, Function.identity(), (left, right) -> left));
	}

	/**
	 * 组装菜单查询条件：菜单ID + 状态 + 生效日期区间 + 名称模糊
	 */
	private Specification<Menu> buildSpecification(MenuQuery q) {
		return (root, criteriaQuery, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (q.getMenuId() != null) {
				predicates.add(cb.equal(root.get("menuId"), q.getMenuId()));
			}
			if (q.getStatus() != null && !q.getStatus().isBlank()) {
				predicates.add(cb.equal(root.get("status"), parseStatus(q.getStatus())));
			}
			if (q.getDateFrom() != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("effectiveDate"), q.getDateFrom()));
			}
			if (q.getDateTo() != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("effectiveDate"), q.getDateTo()));
			}
			if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
				predicates.add(cb.like(cb.lower(root.get("name")),
						"%" + q.getKeyword().trim().toLowerCase() + "%"));
			}
			return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	/**
	 * 按菜品ID组装菜单项：菜品必须存在且 ACTIVE；菜单价为空时取菜品标准单价
	 */
	private List<MenuItem> buildMenuItems(List<ItemRequest> requests) {
		if (requests.isEmpty()) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "菜单至少需要一道菜品");
		}
		List<Long> recipeIds = new ArrayList<>();
		for (ItemRequest request : requests) {
			if (request.recipeId() == null) {
				throw new BusinessException(ErrorCode.PARAM_INVALID, "菜单项必须指定菜品ID");
			}
			if (request.menuPrice() != null && request.menuPrice().compareTo(BigDecimal.ZERO) < 0) {
				throw new BusinessException(ErrorCode.PARAM_INVALID, "菜单价必须为不小于 0 的数字");
			}
			recipeIds.add(request.recipeId());
		}
		Map<Long, Recipe> recipes = recipeRepository.findAllById(recipeIds).stream()
				.collect(Collectors.toMap(Recipe::getRecipeId, Function.identity(), (left, right) -> left));
		List<MenuItem> items = new ArrayList<>();
		Set<Long> seen = new HashSet<>();
		for (ItemRequest request : requests) {
			Recipe recipe = recipes.get(request.recipeId());
			if (recipe == null) {
				throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在，ID：" + request.recipeId());
			}
			if (recipe.getStatus() != RecipeStatus.ACTIVE) {
				throw new BusinessException(ErrorCode.STATE_CONFLICT,
						"菜品已下架，无法加入菜单：" + recipe.getRecipeName());
			}
			if (!seen.add(request.recipeId())) {
				throw new BusinessException(ErrorCode.PARAM_INVALID,
						"同一道菜品不能重复加入菜单：" + recipe.getRecipeName());
			}
			MenuItem item = new MenuItem();
			item.setRecipeId(recipe.getRecipeId());
			item.setRecipeName(recipe.getRecipeName());
			item.setCategory(recipe.getCategory());
			item.setUnit(recipe.getUnit());
			item.setImageUrl(recipe.getImageUrl());
			item.setMenuPrice(request.menuPrice() == null ? recipe.getUnitPrice() : request.menuPrice());
			items.add(item);
		}
		return items;
	}

	/**
	 * 保存菜单项（保存前统一回填 menuId）
	 */
	private void saveMenuItems(Long menuId, List<MenuItem> items) {
		for (MenuItem item : items) {
			item.setMenuId(menuId);
		}
		menuItemRepository.saveAll(items);
	}

	/**
	 * 发布前校验菜品有效性：菜品必须存在且处于 ACTIVE
	 */
	private void validatePublishable(List<MenuItem> items) {
		List<Long> recipeIds = items.stream().map(MenuItem::getRecipeId).filter(id -> id != null).toList();
		Map<Long, Recipe> recipes = recipeRepository.findAllById(recipeIds).stream()
				.collect(Collectors.toMap(Recipe::getRecipeId, Function.identity(), (left, right) -> left));
		for (MenuItem item : items) {
			Recipe recipe = recipes.get(item.getRecipeId());
			if (recipe == null) {
				throw new BusinessException(ErrorCode.NOT_FOUND, "菜单中的菜品不存在，ID：" + item.getRecipeId());
			}
			if (recipe.getStatus() != RecipeStatus.ACTIVE) {
				throw new BusinessException(ErrorCode.STATE_CONFLICT,
						"菜单中的菜品「" + item.getRecipeName() + "」已下架，请先调整菜单再发布");
			}
		}
	}

	/**
	 * 菜单项 → 视图（草稿菜单用：标准单价取当前菜品价，菜单价为空回退标准单价）
	 */
	private List<MenuItemVO> toItemVOsFromMenuItems(List<MenuItem> items, Map<Long, Recipe> recipes) {
		List<MenuItemVO> result = new ArrayList<>();
		for (MenuItem item : items) {
			Recipe recipe = recipes.get(item.getRecipeId());
			BigDecimal unitPrice = recipe == null ? null : recipe.getUnitPrice();
			MenuItemVO vo = new MenuItemVO();
			vo.setItemId(item.getRecipeId());
			vo.setMenuItemId(item.getItemId());
			vo.setRecipeId(item.getRecipeId());
			vo.setRecipeName(item.getRecipeName());
			vo.setCategory(item.getCategory());
			vo.setUnit(item.getUnit());
			vo.setUnitPrice(unitPrice);
			vo.setMenuPrice(resolvePrice(item.getMenuPrice(), unitPrice));
			vo.setImageUrl(item.getImageUrl());
			result.add(vo);
		}
		return result;
	}

	/**
	 * 快照 → 视图（已发布/已下架菜单用：菜名、分类、单位、单价全部来自冻结快照）
	 */
	private List<MenuItemVO> toItemVOsFromSnapshots(List<MenuSnapshot> snapshots, Map<Long, String> imageUrls) {
		List<MenuItemVO> result = new ArrayList<>();
		for (MenuSnapshot snapshot : snapshots) {
			MenuItemVO vo = new MenuItemVO();
			vo.setItemId(snapshot.getRecipeId());
			vo.setRecipeId(snapshot.getRecipeId());
			vo.setRecipeName(snapshot.getRecipeName());
			vo.setCategory(snapshot.getCategory());
			vo.setUnit(snapshot.getUnit());
			vo.setUnitPrice(snapshot.getUnitPrice());
			// 下单取价字段：菜单级价格优先，为空回退菜品标准单价
			vo.setMenuPrice(resolvePrice(snapshot.getMenuPrice(), snapshot.getUnitPrice()));
			vo.setImageUrl(imageUrls.get(snapshot.getRecipeId()));
			result.add(vo);
		}
		return result;
	}

	private MenuVO toVO(Menu menu, List<MenuItemVO> items) {
		MenuVO vo = new MenuVO();
		vo.setMenuId(menu.getMenuId());
		vo.setName(menu.getName());
		vo.setDescription(menu.getDescription());
		MenuStatus status = menu.getStatus() == null ? MenuStatus.DRAFT : menu.getStatus();
		vo.setStatus(status.name());
		vo.setStatusText(status.getLabel());
		vo.setVersion(menu.getVersion());
		vo.setEffectiveDate(menu.getEffectiveDate());
		vo.setPublishedAt(menu.getPublishedAt());
		vo.setOfflineAt(menu.getOfflineAt());
		vo.setOfflineReason(menu.getOfflineReason());
		vo.setLocked(menu.isLocked());
		vo.setCreatedBy(menu.getCreatedBy());
		vo.setCreatedAt(menu.getCreatedAt());
		vo.setUpdatedAt(menu.getUpdatedAt());
		List<MenuItemVO> safeItems = items == null ? List.of() : items;
		vo.setItems(new ArrayList<>(safeItems));
		vo.setItemsByCategory(groupByCategory(safeItems));
		return vo;
	}

	/**
	 * 按分类分组，供点餐页与生产单直接渲染（保持明细原有顺序）
	 */
	private Map<String, List<MenuItemVO>> groupByCategory(List<MenuItemVO> items) {
		Map<String, List<MenuItemVO>> grouped = new LinkedHashMap<>();
		for (MenuItemVO item : items) {
			String category = item.getCategory() == null || item.getCategory().isBlank()
					? UNCATEGORIZED
					: item.getCategory();
			grouped.computeIfAbsent(category, key -> new ArrayList<>()).add(item);
		}
		return grouped;
	}

	/**
	 * 菜品ID → 图片地址（快照表没有 image_url 列，图片取菜单项在组建菜单时冻结的副本）
	 */
	private Map<Long, String> imageUrls(List<MenuItem> items) {
		Map<Long, String> images = new HashMap<>();
		for (MenuItem item : items) {
			if (item.getRecipeId() != null && item.getImageUrl() != null) {
				images.putIfAbsent(item.getRecipeId(), item.getImageUrl());
			}
		}
		return images;
	}

	private Menu requireMenu(Long menuId) {
		if (menuId == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "菜单ID不能为空");
		}
		return menuRepository.findById(menuId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "菜单不存在，ID：" + menuId));
	}

	/**
	 * 乐观锁校验：入参带 version 时与库中不一致即 40902
	 */
	private void checkVersion(Integer current, Integer expected) {
		if (expected != null && !expected.equals(current)) {
			throw new BusinessException(ErrorCode.STATE_CONFLICT, "菜单已被他人修改，请刷新后重试");
		}
	}

	private String requireName(String name) {
		String trimmed = normalize(name);
		if (trimmed == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "菜单名称不能为空");
		}
		return trimmed;
	}

	private LocalDate requireEffectiveDate(LocalDate effectiveDate) {
		if (effectiveDate == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "菜单生效日期不能为空");
		}
		return effectiveDate;
	}

	private MenuStatus parseStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return MenuStatus.valueOf(status.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "未知的菜单状态：" + status);
		}
	}

	private static BigDecimal resolvePrice(BigDecimal menuPrice, BigDecimal unitPrice) {
		return menuPrice == null ? unitPrice : menuPrice;
	}

	private static String label(MenuStatus status) {
		return status == null ? MenuStatus.DRAFT.getLabel() : status.getLabel();
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}

	/**
	 * 菜单项的编排入参（{@code MenuCreateItem} 与 {@code MenuUpdateItem} 的统一内部表示）
	 *
	 * @param recipeId  菜品ID
	 * @param menuPrice 菜单级售价，可为空
	 */
	private record ItemRequest(Long recipeId, BigDecimal menuPrice)
	{
	}
}
