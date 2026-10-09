package com.university.webdesign.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 用户（企业员工）实体。
 * <p>
 * 对应《重构实施规范》第 3 节的 M4 `users` 表：工号唯一、密码 BCrypt 存储，
 * 部门与工位信息供配送单使用，角色通过 `user_role` 关联表维护。
 * 本表由用户与报表中心独占，其他模块只能通过 `UserService` 读取。
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User
{
	/**
	 * 用户ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/**
	 * 工号（登录账号），全局唯一
	 */
	@Column(name = "employee_no", nullable = false, unique = true, length = 50)
	private String employeeNo;

	/**
	 * 密码（BCrypt 哈希）
	 */
	@Column(name = "password", nullable = false, length = 100)
	private String password;

	/**
	 * 员工姓名
	 */
	@Column(name = "name", nullable = false, length = 50)
	private String name;

	/**
	 * 部门ID
	 */
	@Column(name = "dept_id")
	private Long deptId;

	/**
	 * 部门名称快照（与 `dept_id` 同时维护，便于列表展示）
	 */
	@Column(name = "dept_name", length = 50)
	private String deptName;

	/**
	 * 工位（供配送单使用）
	 */
	@Column(name = "workstation", length = 50)
	private String workstation;

	/**
	 * 联系电话
	 */
	@Column(name = "phone", length = 20)
	private String phone;

	/**
	 * 账号状态
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private UserStatus status = UserStatus.ACTIVE;

	/**
	 * 已分配角色
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_role",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new LinkedHashSet<>();

	/**
	 * 是否可登录
	 *
	 * @return 状态为 {@link UserStatus#ACTIVE} 时返回 true
	 */
	public boolean isActive() {
		return status == UserStatus.ACTIVE;
	}

	/**
	 * 取全部角色名称
	 *
	 * @return 角色名称集合，永不为 null
	 */
	public Set<String> roleNames() {
		Set<String> names = new LinkedHashSet<>();
		for (Role role : roles) {
			if (role != null && role.getName() != null) {
				names.add(role.getName());
			}
		}
		return names;
	}
}
