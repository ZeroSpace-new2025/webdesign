package com.university.webdesign.menurecipe.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单 DTO
 */
@Data
public class MenuDTO {

	/**
	 * 菜单ID
	 */
	private Long id;

	/**
	 * 菜单名称
	 */
	private String name;

	/**
	 * 菜单描述
	 */
	private String description;

	/**
	 * 状态：DRAFT / PUBLISHED / OFFLINE
	 */
	private String status;

	/**
	 * 是否已锁定（历史菜单）：发布后锁定，不可编辑或删除
	 */
	private boolean locked;

	/**
	 * 生效时间
	 */
	private LocalDateTime effectiveTime;

	/**
	 * 菜单项列表
	 */
	private List<MenuItemDTO> menuItems;

	/**
	 * 创建者ID
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
	 * 菜单内所有菜品价格合计
	 */
	private BigDecimal totalPrice;
}
