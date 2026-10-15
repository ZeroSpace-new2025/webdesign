package com.university.webdesign.service.user.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入结果。
 * <p>
 * 对应《对外方法表》M4-10 `POST /users/import` 的返回：
 * {@code total}、{@code successCount}、{@code failCount}、{@code errors[{row, reason}]}。
 * <p>
 * 导入支持**部分成功**：单行失败只记录原因并继续处理后续行，不会整批回滚。
 */
@Data
public class ImportResultVO
{
	/**
	 * 数据总行数（不含表头）
	 */
	private int total;

	/**
	 * 成功导入行数
	 */
	private int successCount;

	/**
	 * 失败行数
	 */
	private int failCount;

	/**
	 * 逐行错误明细
	 */
	private List<ImportRowError> errors = new ArrayList<>();

	/**
	 * 记录一条失败行，并同步 failed 计数
	 *
	 * @param row    行号（含表头行，与 Excel 一致）
	 * @param reason 失败原因
	 */
	public void addError(int row, String reason) {
		errors.add(new ImportRowError(row, reason));
		failCount = errors.size();
	}
}
