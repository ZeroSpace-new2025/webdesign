package com.university.webdesign.api.operation;

import lombok.Data;

import java.util.List;

/**
 * 批量打印配送单入参（M3-10）。
 * <p>
 * 对应《对外方法表》M3-10 的 `taskIds[]` 与 `templateId`。
 */
@Data
public class BatchPrintRequest
{
	/**
	 * 要打印的配送任务ID列表，不能为空
	 */
	private List<Long> taskIds;

	/**
	 * 打印模板标识，为空取默认模板
	 */
	private String templateId;
}
