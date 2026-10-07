package com.university.webdesign.menurecipe.service;

import com.university.webdesign.menurecipe.api.MenuDTO;
import com.university.webdesign.menurecipe.api.MenuItemData;
import com.university.webdesign.menurecipe.api.MenuQueryData;
import com.university.webdesign.menurecipe.data.Menu;
import com.university.webdesign.menurecipe.data.MenuItem;
import com.university.webdesign.menurecipe.data.MenuRepository;
import com.university.webdesign.menurecipe.data.Recipe;
import com.university.webdesign.menurecipe.data.RecipeRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 菜单服务实现
 */
@Service
@Transactional
public class MenuServiceImpl implements MenuService {

	private final MenuRepository menuRepository;
	private final RecipeRepository recipeRepository;

	public MenuServiceImpl(MenuRepository menuRepository, RecipeRepository recipeRepository) {
		this.menuRepository = menuRepository;
		this.recipeRepository = recipeRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public MenuDTO getActiveMenu(LocalDate date) {
		if (date == null) {
			return null;
		}
		// 生效规则：取生效时间不晚于当日的已发布菜单，多条时取生效时间最晚的一份
		return menuRepository.findByStatus("PUBLISHED").stream()
				.filter(m -> m.getEffectiveTime() == null || !m.getEffectiveTime().toLocalDate().isAfter(date))
				.max(Comparator.comparing(Menu::getEffectiveTime,
						Comparator.nullsFirst(Comparator.naturalOrder())))
				.map(this::toDTO)
				.orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public MenuDTO getMenu() {
		return getActiveMenu(LocalDate.now());
	}

	@Override
	@Transactional(readOnly = true)
	public MenuDTO getById(Long id) {
		return toDTO(findMenu(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<MenuDTO> query(MenuQueryData q) {
		Specification<Menu> spec = (root, cq, cb) -> {
			List<Predicate> ps = new ArrayList<>();
			if (q.getId() != null) {
				ps.add(cb.equal(root.get("id"), q.getId()));
			}
			if (q.getName() != null && !q.getName().isBlank()) {
				ps.add(cb.like(root.get("name"), "%" + q.getName().trim() + "%"));
			}
			if (q.getStatus() != null && !q.getStatus().isBlank()) {
				ps.add(cb.equal(root.get("status"), q.getStatus().trim()));
			}
			if (q.getCreatedBy() != null && !q.getCreatedBy().isEmpty()) {
				ps.add(root.get("createdBy").in(q.getCreatedBy()));
			}
			if (q.getStartCreatedTime() != null) {
				ps.add(cb.greaterThanOrEqualTo(root.get("createdTime"), toLDT(q.getStartCreatedTime())));
			}
			if (q.getEndCreatedTime() != null) {
				ps.add(cb.lessThanOrEqualTo(root.get("createdTime"), toLDT(q.getEndCreatedTime())));
			}
			if (q.getStartLastModifiedTime() != null) {
				ps.add(cb.greaterThanOrEqualTo(root.get("lastModifiedTime"), toLDT(q.getStartLastModifiedTime())));
			}
			if (q.getEndLastModifiedTime() != null) {
				ps.add(cb.lessThanOrEqualTo(root.get("lastModifiedTime"), toLDT(q.getEndLastModifiedTime())));
			}
			return cb.and(ps.toArray(new Predicate[0]));
		};
		return menuRepository.findAll(spec).stream().map(this::toDTO).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<MenuDTO> getAll() {
		return menuRepository.findAll().stream().map(this::toDTO).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<MenuDTO> getAllMenus() {
		return getAll();
	}

	@Override
	public MenuDTO create(MenuDTO dto) {
		validateMenu(dto);
		Menu menu = new Menu();
		menu.setName(dto.getName().trim());
		menu.setDescription(dto.getDescription());
		menu.setEffectiveTime(dto.getEffectiveTime());
		menu.setStatus("DRAFT");
		if (dto.getCreatedBy() != null) {
			menu.setCreatedBy(dto.getCreatedBy());
		}
		if (dto.getMenuItems() != null) {
			rebuildItems(menu, dto.getMenuItems());
		}
		return toDTO(menuRepository.save(menu));
	}

	@Override
	public MenuDTO update(MenuDTO dto) {
		if (dto.getId() == null) {
			throw new IllegalArgumentException("更新菜单时必须提供菜单ID");
		}
		Menu menu = findMenu(dto.getId());
		if (menu.isLocked()) {
			throw new IllegalStateException("历史菜单已锁定，不可修改；请使用“复制”生成新菜单后再调整");
		}
		if (dto.getName() != null && !dto.getName().isBlank()) {
			menu.setName(dto.getName().trim());
		}
		if (dto.getDescription() != null) {
			menu.setDescription(dto.getDescription());
		}
		if (dto.getEffectiveTime() != null) {
			menu.setEffectiveTime(dto.getEffectiveTime());
		}
		if (dto.getCreatedBy() != null) {
			menu.setCreatedBy(dto.getCreatedBy());
		}
		if (dto.getMenuItems() != null) {
			rebuildItems(menu, dto.getMenuItems());
		}
		return toDTO(menu);
	}

	@Override
	public void delete(Long id) {
		Menu menu = findMenu(id);
		if (menu.isLocked()) {
			throw new IllegalStateException("历史菜单已锁定，不可删除；数据将永久保留");
		}
		menuRepository.delete(menu);
	}

	@Override
	public MenuDTO publish(Long id) {
		Menu menu = findMenu(id);
		if (menu.getMenuItems().isEmpty()) {
			throw new IllegalStateException("菜单内没有任何菜品，无法发布");
		}
		menu.setStatus("PUBLISHED");
		// 首次发布即锁定，此后该菜单成为历史，不再被改动
		menu.setLocked(true);
		if (menu.getEffectiveTime() == null) {
			menu.setEffectiveTime(LocalDateTime.now());
		}
		return toDTO(menu);
	}

	@Override
	public MenuDTO offline(Long id) {
		Menu menu = findMenu(id);
		menu.setStatus("OFFLINE");
		return toDTO(menu);
	}

	@Override
	public MenuDTO copyMenu(Long sourceId) {
		Menu source = findMenu(sourceId);
		Menu copy = new Menu();
		copy.setName(source.getName() + "（副本）");
		copy.setDescription(source.getDescription());
		copy.setStatus("DRAFT");
		copy.setCreatedBy(source.getCreatedBy());
		for (MenuItem srcItem : source.getMenuItems()) {
			MenuItem item = new MenuItem();
			item.setRecipeId(srcItem.getRecipeId());
			item.setRecipeName(srcItem.getRecipeName());
			item.setCategory(srcItem.getCategory());
			item.setUnit(srcItem.getUnit());
			item.setImageUrl(srcItem.getImageUrl());
			item.setPrice(srcItem.getPrice());
			copy.addItem(item);
		}
		return toDTO(menuRepository.save(copy));
	}

	@Override
	public MenuDTO adjustItemPrice(Long menuId, Long itemId, BigDecimal price) {
		Menu menu = findMenu(menuId);
		if (menu.isLocked()) {
			throw new IllegalStateException("历史菜单已锁定，不可调价；请复制后再调整");
		}
		if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("调整后的价格必须为不小于0的数字");
		}
		MenuItem target = menu.getMenuItems().stream()
				.filter(i -> i.getId().equals(itemId))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("菜单中未找到该菜单项，ID: " + itemId));
		target.setPrice(price);
		return toDTO(menu);
	}

	/**
	 * 根据传入的菜单项重建菜单的菜品集合。
	 * 从菜品库读取最新的可用菜品并做快照，仅允许选择 ACTIVE 状态的菜品。
	 */
	private void rebuildItems(Menu menu, List<MenuItemData> items) {
		menu.getMenuItems().clear();
		List<Long> recipeIds = new ArrayList<>();
		for (MenuItemData item : items) {
			if (item.itemId() == null) {
				throw new IllegalArgumentException("菜单项必须指定菜品ID");
			}
			recipeIds.add(item.itemId());
		}
		List<Recipe> recipes = recipeRepository.findAllById(recipeIds);
		for (MenuItemData item : items) {
			Recipe recipe = recipes.stream()
					.filter(r -> r.getRecipeId().equals(item.itemId()))
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException("菜品不存在，ID: " + item.itemId()));
			if (!"ACTIVE".equals(recipe.getStatus())) {
				throw new IllegalStateException("菜品已停用，无法加入新菜单: " + recipe.getRecipeName());
			}
			MenuItem entity = new MenuItem();
			entity.setRecipeId(recipe.getRecipeId());
			entity.setRecipeName(recipe.getRecipeName());
			entity.setCategory(recipe.getCategory());
			entity.setUnit(recipe.getUnit());
			entity.setImageUrl(recipe.getImageUrl());
			entity.setPrice(item.price() != null ? item.price() : recipe.getPrice());
			menu.addItem(entity);
		}
	}

	private void validateMenu(MenuDTO dto) {
		if (dto.getName() == null || dto.getName().isBlank()) {
			throw new IllegalArgumentException("菜单名称不能为空");
		}
	}

	private Menu findMenu(Long id) {
		return menuRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("菜单不存在，ID: " + id));
	}

	private MenuDTO toDTO(Menu m) {
		MenuDTO dto = new MenuDTO();
		dto.setId(m.getId());
		dto.setName(m.getName());
		dto.setDescription(m.getDescription());
		dto.setStatus(m.getStatus());
		dto.setLocked(m.isLocked());
		dto.setEffectiveTime(m.getEffectiveTime());
		dto.setCreatedBy(m.getCreatedBy());
		dto.setCreatedTime(m.getCreatedTime());
		dto.setLastModifiedTime(m.getLastModifiedTime());
		List<MenuItemData> itemDTOs = new ArrayList<>();
		BigDecimal total = BigDecimal.ZERO;
		for (MenuItem item : m.getMenuItems()) {
			itemDTOs.add(toItemDTO(item));
			if (item.getPrice() != null) {
				total = total.add(item.getPrice());
			}
		}
		dto.setMenuItems(itemDTOs);
		dto.setTotalPrice(total);
		return dto;
	}

	private MenuItemData toItemDTO(MenuItem item) {
		return new MenuItemData(item.getRecipeId(), item.getRecipeName(), item.getCategory(), item.getPrice());
	}

	private static LocalDateTime toLDT(long epochMillis) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
	}
}
