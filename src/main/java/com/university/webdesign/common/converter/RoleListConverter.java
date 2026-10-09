package com.university.webdesign.common.converter;

import com.university.webdesign.common.enums.PermissionEnum;
import com.university.webdesign.common.model.PermissionList;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 角色权限位图 ↔ 数据库字符串的转换器。
 * <p>
 * 落库格式为位号字符串，例如 {@code "13,15,17"}；空权限存空串，读回时永远返回非 null 的
 * {@link PermissionList}，避免上层到处判空。
 * <p>
 * 读库时对脏数据宽容：空白、无法解析的位号、枚举里不存在的位号一律跳过，
 * 不让一行脏数据把登录链路整个打断。
 */
@Converter
public class RoleListConverter implements AttributeConverter<PermissionList, String>
{
	/**
	 * 位号分隔符
	 */
	private static final String SEPARATOR = ",";

	@Override
	public String convertToDatabaseColumn(PermissionList attribute) {
		StringBuilder builder = new StringBuilder();
		if (attribute != null) {
			for (PermissionEnum role : attribute) {
				builder.append(role.getCode()).append(SEPARATOR);
			}
			if (!builder.isEmpty()) {
				builder.deleteCharAt(builder.length() - 1);
			}
		}
		return builder.toString();
	}

	@Override
	public PermissionList convertToEntityAttribute(String dbData) {
		PermissionList permissionList = new PermissionList();
		if (dbData == null || dbData.isEmpty()) {
			return permissionList;
		}
		for (String token : dbData.split(SEPARATOR)) {
			String trimmed = token.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			int code;
			try {
				code = Integer.parseInt(trimmed);
			} catch (NumberFormatException exception) {
				// 脏数据（非数字位号）：跳过，不让整行权限读取失败
				continue;
			}
			PermissionEnum permission = PermissionEnum.fromCode(code);
			if (permission != PermissionEnum.None) {
				permissionList.setRoles(permission);
			}
		}
		return permissionList;
	}
}
