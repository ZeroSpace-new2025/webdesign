package com.university.webdesign.user.api;

import lombok.Data;

import java.io.InputStream;

/**
 * Excel批量导入请求。
 */
@Data
public class UserImportData
{
	/**
	 * 原始文件名，用于校验Excel格式
	 */
	private String fileName;

	/**
	 * Excel文件输入流
	 */
	private InputStream inputStream;
}
