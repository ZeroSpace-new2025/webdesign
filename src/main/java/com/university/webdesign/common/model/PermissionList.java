package com.university.webdesign.common.model;

import com.university.webdesign.common.enums.PermissionEnum;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * 角色权限位图。
 * <p>
 * 用 {@link BitSet} 按 {@link PermissionEnum#getCode()} 记录权限，落库时由
 * {@code common.converter.RoleListConverter} 转成 {@code "13,15,17"} 形式的字符串。
 * 位图只存位号，不存编码字符串，因此权限点重命名不影响已授予的数据。
 */
public final class PermissionList implements Iterable<PermissionEnum>
{
	private final BitSet roles = new BitSet();

	/**
	 * 构造空权限列表
	 */
	public PermissionList() {
	}

	/**
	 * 构造并授予若干权限
	 *
	 * @param roles 权限枚举值
	 * @return 权限列表
	 */
	public static PermissionList of(PermissionEnum... roles) {
		PermissionList permissionList = new PermissionList();
		permissionList.setRoles(roles);
		return permissionList;
	}

	/**
	 * 设置权限
	 * <p>
	 * {@link PermissionEnum#None} 只是“未知/无权限”的占位值，不会被写入位图。
	 *
	 * @param roles 权限枚举值
	 */
	public void setRoles(PermissionEnum... roles) {
		if (roles == null) {
			return;
		}
		for (PermissionEnum role : roles) {
			setRole(role);
		}
	}

	/**
	 * 设置权限
	 *
	 * @param roles 权限枚举值集合
	 */
	public void setRoles(Collection<PermissionEnum> roles) {
		if (roles == null) {
			return;
		}
		for (PermissionEnum role : roles) {
			setRole(role);
		}
	}

	/**
	 * 清除权限
	 *
	 * @param roles 权限枚举值
	 */
	public void removeRoles(PermissionEnum... roles) {
		if (roles == null) {
			return;
		}
		for (PermissionEnum role : roles) {
			if (role != null) {
				this.roles.clear(role.getCode());
			}
		}
	}
	
	public void removeRoles(Collection<PermissionEnum> roles) {
		if (roles == null) {
			return;
		}
		for (PermissionEnum role : roles) {
			if (role != null) {
				this.roles.clear(role.getCode());
			}
		}
	}

	/**
	 * 判断是否拥有指定权限中的任意一个
	 *
	 * @param roleCodes 权限枚举值
	 * @return 命中任一权限时返回 true
	 */
	public boolean hasAnyPermission(PermissionEnum... roleCodes) {
		if (roleCodes == null) {
			return false;
		}
		for (PermissionEnum role : roleCodes) {
			if (role != null && roles.get(role.getCode())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断是否拥有指定权限点
	 *
	 * @param roleCode 权限枚举值
	 * @return 拥有时返回 true
	 */
	public boolean hasPermission(PermissionEnum roleCode) {
		return roleCode != null && roles.get(roleCode.getCode());
	}

	/**
	 * 判断是否拥有指定权限点编码（如 {@code report:view}，也接受枚举常量名），忽略大小写
	 *
	 * @param code 权限点编码
	 * @return 拥有时返回 true
	 */
	public boolean hasPermission(String code) {
		if (code == null || code.isBlank()) {
			return false;
		}
		String expected = code.trim();
		for (PermissionEnum permission : this) {
			if (expected.equalsIgnoreCase(permission.displayCode())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 展开为权限点编码集合（细粒度权限点用其 {@code permCode}，模块级枚举值用常量名）
	 *
	 * @return 权限点编码集合，按位号升序
	 */
	public Set<String> toCodes() {
		Set<String> codes = new LinkedHashSet<>();
		for (PermissionEnum permission : this) {
			codes.add(permission.displayCode());
		}
		return codes;
	}

	/**
	 * 是否未授予任何权限
	 *
	 * @return 无权限时返回 true
	 */
	public boolean isEmpty() {
		return this.roles.isEmpty();
	}

	/**
	 * 已授予的权限数量
	 *
	 * @return 权限数量
	 */
	public int size() {
		return this.roles.cardinality();
	}

	public void clearAllRoles() {
		roles.clear();
	}

	@Override
	public String toString() {
		return toCodes().toString();
	}

	@Override
	public Iterator<PermissionEnum> iterator() {
		return new Iterator<>() {
			private int currentIndex = nextValidBit(0);

			@Override
			public boolean hasNext() {
				return currentIndex >= 0;
			}

			@Override
			public PermissionEnum next() {
				if (currentIndex < 0) {
					throw new NoSuchElementException("No more permissions available.");
				}
				PermissionEnum role = PermissionEnum.fromCode(currentIndex);
				currentIndex = nextValidBit(currentIndex + 1);
				return role;
			}
		};
	}

	/**
	 * 写入单个权限位
	 *
	 * @param role 权限枚举值
	 */
	private void setRole(PermissionEnum role) {
		if (role != null && role != PermissionEnum.None) {
			this.roles.set(role.getCode());
		}
	}

	/**
	 * 取下一个有效权限位，跳过未知位号
	 *
	 * @param fromIndex 起始位号
	 * @return 位号；没有更多权限时返回 -1
	 */
	private int nextValidBit(int fromIndex) {
		int index = roles.nextSetBit(fromIndex);
		while (index >= 0) {
			PermissionEnum permission = PermissionEnum.fromCode(index);
			if (permission != PermissionEnum.None) {
				return index;
			}
			index = roles.nextSetBit(index + 1);
		}
		return -1;
	}
}
