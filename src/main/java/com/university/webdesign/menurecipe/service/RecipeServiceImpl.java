package com.university.webdesign.menurecipe.service;

import com.university.webdesign.menurecipe.api.RecipeDTO;
import com.university.webdesign.menurecipe.api.RecipeQueryData;
import com.university.webdesign.menurecipe.data.Recipe;
import com.university.webdesign.menurecipe.data.RecipeRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜品服务实现
 */
@Service
@Transactional
public class RecipeServiceImpl implements RecipeService {

	private final RecipeRepository recipeRepository;
	private final ImageStorageService imageStorageService;

	public RecipeServiceImpl(RecipeRepository recipeRepository, ImageStorageService imageStorageService) {
		this.recipeRepository = recipeRepository;
		this.imageStorageService = imageStorageService;
	}

	@Override
	@Transactional(readOnly = true)
	public RecipeDTO getRecipe(Long recipeId) {
		if (recipeId == null) {
			return null;
		}
		return recipeRepository.findById(recipeId).map(this::toDTO).orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public List<RecipeDTO> getAllRecipes() {
		return getAll();
	}

	@Override
	@Transactional(readOnly = true)
	public RecipeDTO getById(Long id) {
		Recipe recipe = recipeRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("菜品不存在，ID: " + id));
		return toDTO(recipe);
	}

	@Override
	@Transactional(readOnly = true)
	public List<RecipeDTO> query(RecipeQueryData q) {
		Specification<Recipe> spec = (root, cq, cb) -> {
			List<Predicate> ps = new ArrayList<>();
			if (q.getRecipeId() != null) {
				ps.add(cb.equal(root.get("recipeId"), q.getRecipeId()));
			}
			if (q.getRecipeName() != null && !q.getRecipeName().isBlank()) {
				ps.add(cb.like(root.get("recipeName"), "%" + q.getRecipeName().trim() + "%"));
			}
			if (q.getCategory() != null && !q.getCategory().isBlank()) {
				ps.add(cb.equal(root.get("category"), q.getCategory().trim()));
			}
			if (q.getStatus() != null && !q.getStatus().isBlank()) {
				ps.add(cb.equal(root.get("status"), q.getStatus().trim()));
			}
			if (q.getCreatedBy() != null) {
				ps.add(cb.equal(root.get("createdBy"), q.getCreatedBy()));
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
		return recipeRepository.findAll(spec).stream().map(this::toDTO).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<RecipeDTO> getAll() {
		return recipeRepository.findAll().stream().map(this::toDTO).toList();
	}

	@Override
	public RecipeDTO create(RecipeDTO dto) {
		validate(dto, null);
		Recipe recipe = new Recipe();
		applyFields(recipe, dto);
		Recipe saved = recipeRepository.save(recipe);
		return toDTO(saved);
	}

	@Override
	public RecipeDTO update(RecipeDTO dto) {
		if (dto.getRecipeId() == null) {
			throw new IllegalArgumentException("更新菜品时必须提供菜品ID");
		}
		Recipe recipe = recipeRepository.findById(dto.getRecipeId())
				.orElseThrow(() -> new IllegalArgumentException("菜品不存在，ID: " + dto.getRecipeId()));
		validate(dto, dto.getRecipeId());
		applyFields(recipe, dto);
		return toDTO(recipe);
	}

	/**
	 * 数据保护：采用逻辑停用而非物理删除，确保历史菜单、历史订单不受影响。
	 */
	@Override
	public void delete(Long id) {
		Recipe recipe = recipeRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("菜品不存在，ID: " + id));
		recipe.setStatus("INACTIVE");
	}

	@Override
	public String uploadImage(MultipartFile file) {
		return imageStorageService.store(file);
	}

	private void validate(RecipeDTO dto, Long excludeId) {
		if (dto.getRecipeName() == null || dto.getRecipeName().isBlank()) {
			throw new IllegalArgumentException("菜品名称不能为空");
		}
		if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("菜品单价必须为不小于0的数字");
		}
		boolean duplicate = excludeId == null
				? recipeRepository.existsByRecipeName(dto.getRecipeName().trim())
				: recipeRepository.existsByRecipeNameAndRecipeIdNot(dto.getRecipeName().trim(), excludeId);
		if (duplicate) {
			throw new IllegalArgumentException("已存在同名菜品: " + dto.getRecipeName());
		}
	}

	private void applyFields(Recipe recipe, RecipeDTO dto) {
		recipe.setRecipeName(dto.getRecipeName().trim());
		recipe.setCategory(dto.getCategory());
		recipe.setUnit(dto.getUnit());
		recipe.setPrice(dto.getPrice());
		if (dto.getRecipeImageUrl() != null) {
			recipe.setImageUrl(dto.getRecipeImageUrl());
		}
		if (dto.getRecipeDescription() != null) {
			recipe.setDescription(dto.getRecipeDescription());
		}
		if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
			recipe.setStatus(dto.getStatus().trim());
		}
		if (dto.getCreatedBy() != null) {
			recipe.setCreatedBy(dto.getCreatedBy());
		}
	}

	private RecipeDTO toDTO(Recipe r) {
		RecipeDTO dto = new RecipeDTO();
		dto.setRecipeId(r.getRecipeId());
		dto.setRecipeName(r.getRecipeName());
		dto.setCategory(r.getCategory());
		dto.setUnit(r.getUnit());
		dto.setPrice(r.getPrice());
		dto.setRecipeImageUrl(r.getImageUrl());
		dto.setRecipeDescription(r.getDescription());
		dto.setStatus(r.getStatus());
		dto.setCreatedBy(r.getCreatedBy());
		dto.setCreatedTime(r.getCreatedTime());
		dto.setLastModifiedTime(r.getLastModifiedTime());
		return dto;
	}

	private static LocalDateTime toLDT(long epochMillis) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
	}
}
