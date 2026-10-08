package com.university.webdesign.event;

/**
 * 员工信息变更事件（M4 发布，M3 配送信息同步订阅）。
 * <p>
 * 对应《对外方法表》5.3：载荷 `userId`、`deptId`、`workstation`、`phone`。
 *
 * @param userId      用户ID
 * @param deptId      部门ID
 * @param workstation 工位（配送单使用）
 * @param phone       联系电话（配送单使用）
 */
public record UserUpdatedEvent(Long userId, Long deptId, String workstation, String phone)
{
}
