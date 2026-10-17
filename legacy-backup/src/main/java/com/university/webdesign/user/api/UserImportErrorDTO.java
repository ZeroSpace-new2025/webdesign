package com.university.webdesign.user.api;

import lombok.Data;

/**
 * 用户批量导入失败明细。
 */
@Data
public class UserImportErrorDTO
{
	/**
	 * Excel中的行号
	 */
	private int rowNumber;

	/**
	 * 失败原因
	 */
	private String message;
}
