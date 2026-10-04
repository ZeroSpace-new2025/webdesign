package com.university.webdesign.menurecipe.data;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单实体。
 * 对应需求中的 Menu 表：存储菜单头信息（名称、状态、生效时间）。
 * 支持历史菜单复用、菜单发布与下架。
 */
@Entity
@Table(name = "menu")
@Getter
@Setter
public class Menu {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 菜单名称
	 */
	@Column(nullable = false, length = 100)
	private String name;

	/**
	 * 菜单描述
	 */
	@Column(length = 1000)
	private String description;

	/**
	 * 状态：DRAFT（草稿）、PUBLISHED（已发布，供点餐）、OFFLINE（已下架）
	 */
	@Column(length = 20)
	private String status;

	/**
	 * 生效时间
	 */
	private LocalDateTime effectiveTime;

	/**
	 * 历史锁定标记：菜单首次发布后即锁定，此后不可编辑或删除（仅可下架并保留数据）。
	 * 要复用时请“复制”生成新草稿，确保历史菜单与后续变更完全隔离。
	 */
	@Column(name = "locked", nullable = false)
	private boolean locked;

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

	/**
	 * 菜单包含的菜品条目
	 */
	@OneToMany(mappedBy = "menu", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<MenuItem> menuItems = new ArrayList<>();

	public void addItem(MenuItem item) {
		this.menuItems.add(item);
		item.setMenu(this);
	}

	public void removeItem(MenuItem item) {
		this.menuItems.remove(item);
		item.setMenu(null);
	}

	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdTime = now;
		this.lastModifiedTime = now;
		if (this.status == null) {
			this.status = "DRAFT";
		}
	}

	@PreUpdate
	protected void onUpdate() {
		this.lastModifiedTime = LocalDateTime.now();
	}
}
