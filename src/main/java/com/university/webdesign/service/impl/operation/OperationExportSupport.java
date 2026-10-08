package com.university.webdesign.service.impl.operation;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.TabularExport;
import com.university.webdesign.service.order.dto.ExportFormat;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * 运营模块导出文件的组装工具。
 * <p>
 * 对应《对外方法表》M3-03 / M3-12：生产单与配送台账都要能导出文件。
 * 表格与文本的实际生成统一交给公共设施 {@link TabularExport}
 * （CSV / SpreadsheetML 2003 XML，离线环境不引入 POI），本类只负责：
 * <ul>
 *     <li>把 {@link ExportFormat} 映射到具体格式；</li>
 *     <li>给资源带上文件名，供 api 层拼 {@code Content-Disposition}；</li>
 *     <li>PDF 未实现时抛 40001 并提示可用格式。</li>
 * </ul>
 */
public final class OperationExportSupport
{
	/**
	 * 生成表格类导出文件资源
	 *
	 * @param format   导出格式
	 * @param baseName 文件名前缀（不含后缀）
	 * @param headers  表头
	 * @param rows     数据行
	 * @return 文件资源
	 */
	public static Resource render(ExportFormat format, String baseName, List<String> headers, List<List<String>> rows) {
		ExportFormat effective = format == null ? ExportFormat.XLSX : format;
		return switch (effective) {
			case CSV -> named(TabularExport.csv(headers, rows), baseName + ".csv");
			case XLSX -> named(TabularExport.xlsx("导出数据", headers, rows), baseName + ".xls");
			case PDF -> throw new BusinessException(ErrorCode.PARAM_INVALID,
					"暂不支持 PDF 导出（未引入 PDF 生成库），请使用 XLSX 或 CSV");
		};
	}

	/**
	 * 生成带文件名的纯文本资源（生产单 / 配送单直接送打印机的场景）
	 *
	 * @param fileName 文件名（含后缀）
	 * @param text     文本内容
	 * @return 文件资源
	 */
	public static Resource text(String fileName, String text) {
		return named(TabularExport.text(text), fileName);
	}

	/**
	 * 给资源补上文件名（{@link TabularExport} 返回的资源不带文件名）
	 *
	 * @param resource 原资源
	 * @param fileName 文件名
	 * @return 带文件名的资源
	 */
	private static Resource named(Resource resource, String fileName) {
		ByteArrayResource named = new ByteArrayResource(read(resource)) {
			@Override
			public String getFilename() {
				return fileName;
			}
		};
		return named;
	}

	private static byte[] read(Resource resource) {
		try {
			return resource.getInputStream().readAllBytes();
		} catch (java.io.IOException exception) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR,
					"生成导出文件失败：" + exception.getMessage(), exception);
		}
	}
}
