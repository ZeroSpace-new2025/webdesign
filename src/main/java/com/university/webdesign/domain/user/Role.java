package com.university.webdesign.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 角色实体。
 * <p>
 * 对应《重构实施规范》第 3 节的 M4 `roles` 表：角色编码唯一（如 {@code MANAGER}），
 * 通过 `role_permission` 关联表挂权限点。角色与用户是多对多关系。
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role
{
	/**
	 * 角色ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/**
	 * 角色编码，全局唯一
	 */
	@Column(name = "role_code", nullable = false, unique = true, length = 50)
	private String roleCode;

	/**
	 * 角色名称
	 */
	@Column(name = "role_name", nullable = false, length = 50)
	private String roleName;

	/**
	 * 角色描述
	 */
	@Column(name = "description", length = 255)
	private String description;

	/**
	 * 已授予的权限点
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "role_permission",
			joinColumns = @JoinColumn(name = "role_id"),
			inverseJoinColumns = @JoinColumn(name = "permission_id"))
	private Set<Permission> permissions = new LinkedHashSet<>();

	/**
	 * 取全部权限点编码
	 *
	 * @return 权限点编码集合，永不为 null
	 */
	public Set<String> permCodes() {
		Set<String> codes = new LinkedHashSet<>();
		for (Permission permission : permissions) {
			if (permission != null && permission.getPermCode() != null) {
				codes.add(permission.getPermCode());
			}
		}
		return codes;
	}
}
