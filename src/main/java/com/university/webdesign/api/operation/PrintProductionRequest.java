package com.university.webdesign.api.operation;

import com.university.webdesign.service.operation.dto.PrintFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 生产单打印入参（M3-03）。
 * <p>
 * 对应《对外方法表》M3-03 的 `date`、`categoryIds[]`、`format=PDF|TXT|PRINT`。
 */
@Data
public class PrintProductionRequest
{
	/**
	 * 汇总日期，为空取当天
	 */
	private LocalDate date;

	/**
	 * 指定分类ID，为空表示全部分类（当前实现按分类名匹配）
	 */
	private List<Long> categoryIds;

	/**
	 * 打印格式：TXT（默认）/ XLSX / PRINT / PDF（未实现，返回 40001）
	 */
	@NotNull(message = "打印格式不能为空")
	private PrintFormat format;
}
