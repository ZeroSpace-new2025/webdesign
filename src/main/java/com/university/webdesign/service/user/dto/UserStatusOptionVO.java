package com.university.webdesign.service.user.dto;

/**
 * 账号状态选项。
 * <p>
 * 供 api 层与页面渲染“启用/停用/锁定”下拉框使用，取值与
 * {@link com.university.webdesign.domain.user.UserStatus} 保持一致。
 *
 * @param code 状态编码
 * @param text 中文文案
 */
public record UserStatusOptionVO(String code, String text)
{
}
