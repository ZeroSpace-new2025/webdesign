package com.university.webdesign.api.operation;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 运营模块文件下载响应的组装工具。
 * <p>
 * 对应《对外方法表》M3-03 / M3-12 的“返回文件流（{@code Content-Disposition}）”约定：
 * 文件类接口成功时**不包** {@code Result}，直接用 {@code ResponseEntity<byte[]>} 返回，
 * 中文文件名同时给出 {@code filename} 与 {@code filename*} 两种写法以兼容各浏览器。
 */
public final class OperationFileResponses
{
	/**
	 * Excel（SpreadsheetML 2003 XML）响应类型
	 */
	private static final MediaType EXCEL = MediaType.parseMediaType("application/vnd.ms-excel");

	/**
	 * 纯文本（UTF-8）响应类型
	 */
	private static final MediaType TEXT = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

	private OperationFileResponses() {
	}

	/**
	 * 组装文件下载响应
	 *
	 * @param resource  文件资源
	 * @param excel     是否为表格类文件
	 * @param defaultName 默认文件名（资源未带文件名时使用）
	 * @return 文件下载响应
	 */
	public static ResponseEntity<byte[]> download(Resource resource, boolean excel, String defaultName) {
		String fileName = resource.getFilename() == null ? defaultName : resource.getFilename();
		String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
		return ResponseEntity.ok()
				.contentType(excel ? EXCEL : TEXT)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
				.body(readBytes(resource));
	}

	/**
	 * 读取资源内容
	 *
	 * @param resource 文件资源
	 * @return 文件字节
	 */
	public static byte[] readBytes(Resource resource) {
		try {
			return resource.getInputStream().readAllBytes();
		} catch (IOException exception) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR,
					"生成导出文件失败：" + exception.getMessage(), exception);
		}
	}
}
