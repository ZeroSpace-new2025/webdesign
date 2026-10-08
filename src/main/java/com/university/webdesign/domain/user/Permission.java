package com.university.webdesign.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 权限点实体。
 * <p>
 * 对应《重构实施规范》第 3 节的 M4 `permissions` 表：权限点编码唯一（如 {@code report:view}），
 * 按所属模块（menu / order / operation / user / report）分组，便于前端按模块渲染权限树。
 * 编码常量集中在 {@code com.university.webdesign.common.PermCodes}。
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class Permission
{
	/**
	 * 权限点ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/**
	 * 权限点编码，全局唯一
	 */
	@Column(name = "perm_code", nullable = false, unique = true, length = 100)
	private String permCode;

	/**
	 * 权限点名称
	 */
	@Column(name = "perm_name", nullable = false, length = 50)
	private String permName;

	/**
	 * 所属模块（menu / order / operation / user / report）
	 */
	@Column(name = "module", length = 50)
	private String module;

	/**
	 * 权限点说明
	 */
	@Column(name = "description", length = 255)
	private String description;
}
