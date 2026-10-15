package com.university.webdesign.event;

import java.util.List;

/**
 * 角色权限变更事件（M4 发布，全模块订阅）。
 * <p>
 * 对应《对外方法表》5.3：角色权限调整或用户角色调整后发布，各模块据此失效鉴权缓存。
 *
 * @param roleId    角色ID，用户角色调整时可为 null
 * @param userId    用户ID，角色权限调整时可为 null
 * @param permCodes 变更后的权限点编码集合
 */
public record RolePermissionChangedEvent(Long roleId, Long userId, List<String> permCodes)
{
}
