package com.university.webdesign.service.impl.menu;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.domain.menu.MenuStatus;
import com.university.webdesign.domain.menu.Recipe;
import com.university.webdesign.domain.menu.RecipeStatus;
import com.university.webdesign.event.RecipeChangedEvent;
import com.university.webdesign.repository.menu.MenuItemRepository;
import com.university.webdesign.repository.menu.RecipeRepository;
import com.university.webdesign.service.menu.RecipeService;
import com.university.webdesign.service.menu.dto.CategoryVO;
import com.university.webdesign.service.menu.dto.RecipeCreateCmd;
import com.university.webdesign.service.menu.dto.RecipeQuery;
import com.university.webdesign.service.menu.dto.RecipeUpdateCmd;
import com.university.webdesign.service.menu.dto.RecipeVO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜品（食谱）服务实现（M1-01 ~ M1-07）。
 * <p>
 * 关键规则：
 * <ul>
 *     <li>菜品名唯一：新增/改名撞车抛 40901；</li>
 *     <li>乐观锁：入参 {@code version} 与库中不一致抛 40902；</li>
 *     <li>数据保护：下架只置 {@code DISABLED}，被“已发布菜单”引用时抛 42203 并提示先下架菜单；</li>
 *     <li>身份：{@code createdBy} 取 {@code UserContextHolder.currentUserId()}，无上下文时留空；</li>
 *     <li>事件：新增/修改/下架后发布 {@code RecipeChangedEvent}（监听方用 AFTER_COMMIT 订阅）。</li>
 * </ul>
 * 事务统一普通 {@code @Transactional}，查询也不加 {@code readOnly = true}。
 */
@Service
@Transactional
public class RecipeServiceImpl implements RecipeService
{
	private final RecipeRepository recipeRepository;

	private final MenuItemRepository menuItemRepository;

	private final ImageStorageService imageStorageService;

	private final ApplicationEventPublisher eventPublisher;

	public RecipeServiceImpl(
			RecipeRepository recipeRepository,
			MenuItemRepository menuItemRepository,
			ImageStorageService imageStorageService,
			ApplicationEventPublisher eventPublisher) {
		this.recipeRepository = recipeRepository;
		this.menuItemRepository = menuItemRepository;
		this.imageStorageService = imageStorageService;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public Long create(RecipeCreateCmd cmd) {
		if (cmd == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "请求体不能为空");
		}
		String name = requireName(cmd.getName());
		BigDecimal unitPrice = requireUnitPrice(cmd.getUnitPrice());
		if (recipeRepository.existsByRecipeName(name)) {
			throw new BusinessException(ErrorCode.DUPLICATED, "已存在同名菜品：" + name);
		}
		Recipe recipe = new Recipe();
		recipe.setRecipeName(name);
		recipe.setCategory(resolveCategory(cmd.getCategory(), cmd.getCategoryId()));
		recipe.setUnit(normalize(cmd.getUnit()));
		recipe.setUnitPrice(unitPrice);
		recipe.setImageUrl(normalize(cmd.getImageUrl()));
		recipe.setDescription(normalize(cmd.getDescription()));
		recipe.setStatus(RecipeStatus.ACTIVE);
		recipe.setCreatedBy(UserContextHolder.currentUserId());
		LocalDateTime now = LocalDateTime.now();
		recipe.setCreatedAt(now);
		recipe.setUpdatedAt(now);
		Recipe saved = recipeRepository.save(recipe);
		eventPublisher.publishEvent(new RecipeChangedEvent(saved.getRecipeId(),
				RecipeChangedEvent.CREATED, LocalDateTime.now()));
		return saved.getRecipeId();
	}

	@Override
	public void update(Long recipeId, RecipeUpdateCmd cmd) {
		if (cmd == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "请求体不能为空");
		}
		Recipe recipe = requireRecipe(recipeId);
		checkVersion(recipe.getVersion(), cmd.getVersion());
		if (cmd.getName() != null) {
			String name = requireName(cmd.getName());
			if (recipeRepository.existsByRecipeNameAndRecipeIdNot(name, recipeId)) {
				throw new BusinessException(ErrorCode.DUPLICATED, "已存在同名菜品：" + name);
			}
			recipe.setRecipeName(name);
		}
		String category = resolveCategory(cmd.getCategory(), cmd.getCategoryId());
		if (category != null) {
			recipe.setCategory(category);
		}
		if (cmd.getUnit() != null) {
			recipe.setUnit(normalize(cmd.getUnit()));
		}
		if (cmd.getUnitPrice() != null) {
			recipe.setUnitPrice(requireUnitPrice(cmd.getUnitPrice()));
		}
		if (cmd.getImageUrl() != null) {
			recipe.setImageUrl(normalize(cmd.getImageUrl()));
		}
		if (cmd.getDescription() != null) {
			recipe.setDescription(normalize(cmd.getDescription()));
		}
		if (cmd.getStatus() != null && !cmd.getStatus().isBlank()) {
			recipe.setStatus(parseStatus(cmd.getStatus()));
			if (recipe.getStatus() == RecipeStatus.ACTIVE) {
				recipe.setOfflineReason(null);
			}
		}
		recipe.setUpdatedAt(LocalDateTime.now());
		recipeRepository.saveAndFlush(recipe);
		eventPublisher.publishEvent(new RecipeChangedEvent(recipeId,
				RecipeChangedEvent.UPDATED, LocalDateTime.now()));
	}

	@Override
	public int disable(Long recipeId, String reason) {
		Recipe recipe = requireRecipe(recipeId);
		long publishedRefs = menuItemRepository.countMenusReferencing(recipeId, MenuStatus.PUBLISHED);
		if (publishedRefs > 0) {
			throw new BusinessException(ErrorCode.DATA_PROTECTED,
					"菜品「" + recipe.getRecipeName() + "」已被 " + publishedRefs
							+ " 份已发布菜单引用，请先下架这些菜单再下架菜品");
		}
		recipe.setStatus(RecipeStatus.DISABLED);
		recipe.setOfflineReason(truncate(normalize(reason), 200));
		recipe.setUpdatedAt(LocalDateTime.now());
		recipeRepository.saveAndFlush(recipe);
		eventPublisher.publishEvent(new RecipeChangedEvent(recipeId,
				RecipeChangedEvent.DISABLED, LocalDateTime.now()));
		// 受影响菜单数：已发布菜单会因“被引用”直接拦截，已下架菜单有冻结快照不受影响，
		// 因此真正受影响的是仍可编辑的草稿菜单。
		return (int) menuItemRepository.countMenusReferencing(recipeId, MenuStatus.DRAFT);
	}

	@Override
	public RecipeVO getById(Long recipeId) {
		return toVO(requireRecipe(recipeId));
	}

	@Override
	public PageResult<RecipeVO> page(RecipeQuery q) {
		RecipeQuery query = q == null ? new RecipeQuery() : q;
		Pageable pageable = PageRequest.of(query.toSpringPageNumber(), query.normalizedPageSize(),
				Sort.by(Sort.Direction.DESC, "recipeId"));
		Page<Recipe> page = recipeRepository.findAll(buildSpecification(query), pageable);
		return PageResult.from(page, this::toVO);
	}

	@Override
	public String uploadImage(MultipartFile file) {
		return imageStorageService.store(file);
	}

	@Override
	public List<CategoryVO> listCategories() {
		// 分类字典当前由 recipe.category 去重派生（未来可独立建分类表，categoryId 语义不变）
		return recipeRepository.findDistinctCategories().stream()
				.filter(name -> name != null && !name.isBlank())
				.map(name -> new CategoryVO(categoryIdOf(name), name, recipeRepository.countByCategory(name)))
				.toList();
	}

	/**
	 * 组装分页查询条件：名称模糊 + 分类（名称或派生分类ID）+ 状态 + 价格区间
	 */
	private Specification<Recipe> buildSpecification(RecipeQuery q) {
		return (root, criteriaQuery, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
				predicates.add(cb.like(cb.lower(root.get("recipeName")),
						"%" + q.getKeyword().trim().toLowerCase() + "%"));
			}
			if (q.getCategory() != null && !q.getCategory().isBlank()) {
				predicates.add(cb.equal(root.get("category"), q.getCategory().trim()));
			} else if (q.getCategoryId() != null) {
				List<String> categoryNames = matchCategoryNames(q.getCategoryId());
				// 分类不存在时用恒假条件返回空页，而不是报错（筛选条件允许查不到）
				predicates.add(categoryNames.isEmpty()
						? cb.disjunction()
						: root.get("category").in(categoryNames));
			}
			if (q.getStatus() != null && !q.getStatus().isBlank()) {
				predicates.add(cb.equal(root.get("status"), parseStatus(q.getStatus())));
			}
			if (q.getMinPrice() != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("unitPrice"), q.getMinPrice()));
			}
			if (q.getMaxPrice() != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("unitPrice"), q.getMaxPrice()));
			}
			return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	/**
	 * 菜品分类：直接给分类名时按其登记（派生分类允许新分类名），只给分类ID时必须能解析出分类名
	 */
	private String resolveCategory(String category, Long categoryId) {
		String trimmed = normalize(category);
		if (trimmed != null) {
			if (categoryId != null && !categoryId.equals(categoryIdOf(trimmed))) {
				throw new BusinessException(ErrorCode.PARAM_INVALID,
						"分类ID与分类名称不匹配：" + categoryId + " / " + trimmed);
			}
			return trimmed;
		}
		if (categoryId == null) {
			return null;
		}
		return resolveCategoryNames(categoryId).stream()
				.findFirst()
				.orElseThrow(() -> new BusinessException(ErrorCode.PARAM_INVALID, "分类不存在：" + categoryId));
	}

	/**
	 * 由派生分类ID反查分类名（哈希可能撞车，因此返回全部匹配项）
	 */
	private List<String> matchCategoryNames(Long categoryId) {
		return recipeRepository.findDistinctCategories().stream()
				.filter(name -> name != null && categoryId.equals(categoryIdOf(name)))
				.toList();
	}

	/**
	 * 由派生分类ID反查分类名，解析不到即抛 40001
	 */
	private List<String> resolveCategoryNames(Long categoryId) {
		List<String> names = matchCategoryNames(categoryId);
		if (names.isEmpty()) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "分类不存在：" + categoryId);
		}
		return names;
	}

	private Recipe requireRecipe(Long recipeId) {
		if (recipeId == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "菜品ID不能为空");
		}
		return recipeRepository.findById(recipeId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在，ID：" + recipeId));
	}

	/**
	 * 乐观锁校验：入参带 version 时与库中不一致即 40902
	 */
	private void checkVersion(Integer current, Integer expected) {
		if (expected != null && !expected.equals(current)) {
			throw new BusinessException(ErrorCode.STATE_CONFLICT, "菜品已被他人修改，请刷新后重试");
		}
	}

	private String requireName(String name) {
		String trimmed = normalize(name);
		if (trimmed == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "菜品名称不能为空");
		}
		return trimmed;
	}

	private BigDecimal requireUnitPrice(BigDecimal unitPrice) {
		if (unitPrice == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "菜品单价不能为空");
		}
		if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "菜品单价必须为不小于 0 的数字");
		}
		return unitPrice;
	}

	private RecipeStatus parseStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return RecipeStatus.valueOf(status.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "未知的菜品状态：" + status);
		}
	}

	/**
	 * 派生分类ID：分类名的稳定哈希（未来独立分类表后可替换为表主键）
	 */
	private static long categoryIdOf(String categoryName) {
		return (long) Math.abs(categoryName.hashCode());
	}

	private RecipeVO toVO(Recipe recipe) {
		RecipeVO vo = new RecipeVO();
		vo.setRecipeId(recipe.getRecipeId());
		vo.setName(recipe.getRecipeName());
		vo.setCategory(recipe.getCategory());
		vo.setCategoryId(recipe.getCategory() == null ? null : categoryIdOf(recipe.getCategory()));
		vo.setUnit(recipe.getUnit());
		vo.setUnitPrice(recipe.getUnitPrice());
		vo.setImageUrl(recipe.getImageUrl());
		vo.setDescription(recipe.getDescription());
		RecipeStatus status = recipe.getStatus() == null ? RecipeStatus.ACTIVE : recipe.getStatus();
		vo.setStatus(status.name());
		vo.setStatusText(status.getLabel());
		vo.setVersion(recipe.getVersion());
		vo.setCreatedBy(recipe.getCreatedBy());
		vo.setCreatedAt(recipe.getCreatedAt());
		vo.setUpdatedAt(recipe.getUpdatedAt());
		return vo;
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
}
