package com.university.webdesign.domain.user;

import com.university.webdesign.common.converter.RoleListConverter;
import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.common.model.PermissionList;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * 角色实体。
 * <p>
 * 对应 `role` 表：{@code id} 主键、{@code name} 业务名称（全局唯一）、
 * {@code permission_list} 权限位图（{@link PermissionList}，落库为 {@code "13,15"} 形式的字符串）。
 * <p>
 * 角色不再与权限点表做多对多：权限点由 {@link PermissionEnum} 表达，角色只保存位图，
 * 因此授权变更不需要维护关联表，历史位图也不受权限点改名影响。
 */
@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role
{
	/**
	 * 角色ID（主键）
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/**
	 * 角色业务名称，全局唯一（预置角色取 {@code common.RoleCodes} 的取值）
	 */
	@Column(name = "name", nullable = false, unique = true, length = 50)
	private String name;

	/**
	 * 角色权限位图，永不为 null
	 */
	@Convert(converter = RoleListConverter.class)
	@Column(name = "permission_list", nullable = false, length = 500)
	private PermissionList permissionList = new PermissionList();

	/**
	 * 取权限位图（非 null）
	 *
	 * @return 权限位图
	 */
	public PermissionList permissions() {
		if (permissionList == null) {
			permissionList = new PermissionList();
		}
		return permissionList;
	}

	/**
	 * 取全部权限点编码（细粒度权限点用其 {@code permCode}，模块级枚举值用常量名）
	 *
	 * @return 权限点编码集合，永不为 null
	 */
	public Set<String> permCodes() {
		return permissions().toCodes();
	}

	/**
	 * 判断是否拥有指定权限点编码
	 *
	 * @param permCode 权限点编码
	 * @return 拥有时返回 true
	 */
	public boolean hasPermission(String permCode) {
		return permissions().hasPermission(permCode);
	}
}
