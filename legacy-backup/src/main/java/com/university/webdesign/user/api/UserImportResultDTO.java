package com.university.webdesign.user.api;

import lombok.Data;

import java.util.List;

/**
 * 用户批量导入结果。
 */
@Data
public class UserImportResultDTO
{
	/**
	 * 导入数据总行数
	 */
	private int totalCount;

	/**
	 * 成功导入行数
	 */
	private int successCount;

	/**
	 * 导入失败行数
	 */
	private int failureCount;

	/**
	 * 导入失败明细
	 */
	private List<UserImportErrorDTO> errors;
}
