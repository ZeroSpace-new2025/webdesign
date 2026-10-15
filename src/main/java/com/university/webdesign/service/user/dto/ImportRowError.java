package com.university.webdesign.service.user.dto;

/**
 * 批量导入时的单行错误。
 * <p>
 * 对应《对外方法表》M4-10 的 {@code errors[{row, reason}]}。
 *
 * @param row    行号（从 1 开始，含表头行，与用户看到的 Excel 行号一致）
 * @param reason 中文失败原因
 */
public record ImportRowError(int row, String reason)
{
}
