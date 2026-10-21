package com.university.webdesign.config;

import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.enums.PermissionEnum;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 临时硬编码管理员账号。
 * <p>
 * 用于解决开发初期数据库没初始化、或者 M4 模块还没写完时，系统能有一个兜底的管理员账号登录。
 * 只要工号和密码匹配，就能直接签发最高权限的 Token。
 */
@Component
public class TemporaryAdminAccount
{
    /**
     * 固定用户ID，避免与数据库中真实用户冲突
     */
    public static final Long USER_ID = -1L;

    /**
     * 显示名称
     */
    public static final String NAME = "临时管理员";

    /**
     * 临时账号工号（满足组员的 record 式调用）
     */
    public String employeeNo() {
        return "admin";
    }

    /**
     * 临时账号密码
     */
    public String password() {
        return "admin123";
    }

    /**
     * 角色（直接赋予经理角色）
     */
    public List<String> roles() {
        return List.of(RoleCodes.MANAGER);
    }

    /**
     * 权限点（赋予所有可用的权限）
     */
    public List<String> permCodes() {
        return PermissionEnum.dictionary().stream()
                .map(PermissionEnum::displayCode)
                .toList();
    }

    /**
     * 判断账号密码是否匹配（组员的 AuthServiceImpl 会调用这个）
     */
    public boolean matches(String inputNo, String inputPwd) {
        return employeeNo().equalsIgnoreCase(inputNo) && password().equals(inputPwd);
    }

    /**
     * 判断是否是该临时账号
     */
    public boolean isAdmin(Long userId) {
        return USER_ID.equals(userId);
    }
}