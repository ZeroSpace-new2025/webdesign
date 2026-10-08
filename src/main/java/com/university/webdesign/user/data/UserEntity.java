package com.university.webdesign.user.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 用户表，user_role是用户与角色的映射表。
 */
@Entity
@Table(name = "users", indexes = @Index(name = "idx_user_department", columnList = "department"))
@Getter
@Setter
@NoArgsConstructor
public class UserEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 80)
	private String username;

	@Column(nullable = false, length = 120)
	private String password;

	@Column(nullable = false, length = 80)
	private String name;

	@Column(length = 80)
	private String department;

	@Column(length = 80)
	private String workstation;

	@Column(length = 30)
	private String phone;

	@Column(nullable = false)
	private Boolean enabled = true;

	@Column(nullable = false)
	private Long createdTime;

	@Column(nullable = false)
	private Long lastModifiedTime;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "user_role",
		joinColumns = @JoinColumn(name = "user_id"),
		inverseJoinColumns = @JoinColumn(name = "role_id"))
	@OrderBy("id ASC")
	private Set<RoleEntity> roles = new LinkedHashSet<>();

	@PrePersist
	private void onCreate() {
		long now = System.currentTimeMillis();
		createdTime = now;
		lastModifiedTime = now;
	}

	@PreUpdate
	private void onUpdate() {
		lastModifiedTime = System.currentTimeMillis();
	}
}
