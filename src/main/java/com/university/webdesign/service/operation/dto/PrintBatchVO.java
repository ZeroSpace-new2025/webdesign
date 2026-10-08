package com.university.webdesign.service.operation.dto;

import lombok.Data;

/**
 * 批量打印结果。
 * <p>
 * 对应《对外方法表》M3-10：返回打印批次号与本次打印数量。
 */
@Data
public class PrintBatchVO
{
	/**
	 * 打印批次号
	 */
	private String printBatchId;

	/**
	 * 本次打印的任务数
	 */
	private int printedCount;

	/**
	 * 打印内容（纯文本渲染结果，供 `PRINT` 格式直接推送打印机）
	 */
	private String content;
}
