package com.university.webdesign.menurecipe.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品（食谱）实体。
 * 对应需求中的 Recipe 表：存储标准菜品信息（名称、分类、图片、计量单位、单价）。
 * 修改或删除菜品时，仅影响未来菜单，不影响历史订单数据。
 */
@Entity
@Table(name = "recipe")
@Getter
@Setter
public class Recipe {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long recipeId;

	/**
	 * 菜品名称
	 */
	@Column(nullable = false, length = 100)
	private String recipeName;

	/**
	 * 菜品分类，如：荤菜、素菜、主食
	 */
	@Column(length = 50)
	private String category;

	/**
	 * 计量单位，如：份、两、个
	 */
	@Column(length = 20)
	private String unit;

	/**
	 * 标准单价
	 */
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	/**
	 * 菜品图片访问地址
	 */
	@Column(length = 500)
	private String imageUrl;

	/**
	 * 菜品描述
	 */
	@Column(length = 1000)
	private String description;

	/**
	 * 状态：ACTIVE（可用于新菜单）、INACTIVE（停用，不再用于新菜单但保留历史数据）
	 */
	@Column(length = 20)
	private String status;

	/**
	 * 创建者用户ID
	 */
	private Long createdBy;

	/**
	 * 创建时间
	 */
	private LocalDateTime createdTime;

	/**
	 * 最后修改时间
	 */
	private LocalDateTime lastModifiedTime;

	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdTime = now;
		this.lastModifiedTime = now;
		if (this.status == null) {
			this.status = "ACTIVE";
		}
	}

	@PreUpdate
	protected void onUpdate() {
		this.lastModifiedTime = LocalDateTime.now();
	}
}
