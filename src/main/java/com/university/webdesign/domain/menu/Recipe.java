package com.university.webdesign.domain.menu;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品（食谱）实体，对应表 {@code recipe}。
 * <p>
 * 存储“卖什么”的标准信息：名称、分类、计量单位、单价、图片、描述。
 * 修改或下架菜品只影响未来菜单：已发布菜单的价格与菜名已在
 * {@link MenuSnapshot} 中冻结，历史订单明细另有冗余快照。
 * <p>
 * 字段与《重构实施规范》第 3 节一致（显式下划线列名）；
 * {@code created_at}/{@code updated_at} 由 service 层显式赋值，便于版本号自增时机可控。
 */
@Entity
@Table(name = "recipe")
@Getter
@Setter
@NoArgsConstructor
public class Recipe
{
	/**
	 * 菜品ID（主键）
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "recipe_id")
	private Long recipeId;

	/**
	 * 菜品名称，全局唯一（重复抛 40901）
	 */
	@Column(name = "recipe_name", nullable = false, length = 100)
	private String recipeName;

	/**
	 * 菜品分类名，如：荤菜、素菜、主食（分类字典当前由本列去重派生）
	 */
	@Column(name = "category", length = 50)
	private String category;

	/**
	 * 计量单位，如：份、两、个
	 */
	@Column(name = "unit", length = 20)
	private String unit;

	/**
	 * 标准单价（元）
	 */
	@Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
	private BigDecimal unitPrice;

	/**
	 * 菜品图片访问地址，如 {@code /uploads/xxx.png}
	 */
	@Column(name = "image_url", length = 500)
	private String imageUrl;

	/**
	 * 菜品描述
	 */
	@Column(name = "description", length = 1000)
	private String description;

	/**
	 * 状态：{@link RecipeStatus#ACTIVE} 可售 / {@link RecipeStatus#DISABLED} 已下架
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private RecipeStatus status = RecipeStatus.ACTIVE;

	/**
	 * 下架原因（M1-03 入参 {@code reason}），仅下架时写入，用于留痕
	 */
	@Column(name = "offline_reason", length = 200)
	private String offlineReason;

	/**
	 * 乐观锁版本号：更新接口入参 version 与库中不一致时抛 40902
	 */
	@Version
	@Column(name = "version", nullable = false)
	private Integer version;

	/**
	 * 创建者用户ID，取 {@code UserContextHolder.currentUserId()}，无登录上下文时为空
	 */
	@Column(name = "created_by")
	private Long createdBy;

	/**
	 * 创建时间
	 */
	@Column(name = "created_at")
	private LocalDateTime createdAt;

	/**
	 * 最后修改时间
	 */
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
}
