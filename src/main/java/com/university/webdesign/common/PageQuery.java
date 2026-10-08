package com.university.webdesign.common;

import lombok.Data;

/**
 * 统一分页入参。
 * <p>
 * 对应《对外方法表》1.4 通用约定：{@code PageQuery{pageNum, pageSize}}，{@code pageSize}
 * 上限 100。页码从 1 开始计数。
 */
@Data
public class PageQuery
{
	/**
	 * 每页条数上限
	 */
	public static final int MAX_PAGE_SIZE = 100;

	/**
	 * 默认每页条数
	 */
	public static final int DEFAULT_PAGE_SIZE = 20;

	/**
	 * 页码，从 1 开始
	 */
	private Integer pageNum = 1;

	/**
	 * 每页条数，上限 {@value #MAX_PAGE_SIZE}
	 */
	private Integer pageSize = DEFAULT_PAGE_SIZE;

	/**
	 * 归一化后的页码（至少为 1）
	 *
	 * @return 页码
	 */
	public int normalizedPageNum() {
		return pageNum == null || pageNum < 1 ? 1 : pageNum;
	}

	/**
	 * 归一化后的每页条数（1 ~ {@value #MAX_PAGE_SIZE}）
	 *
	 * @return 每页条数
	 */
	public int normalizedPageSize() {
		if (pageSize == null || pageSize < 1) {
			return DEFAULT_PAGE_SIZE;
		}
		return Math.min(pageSize, MAX_PAGE_SIZE);
	}

	/**
	 * 转换为 Spring Data 的页码（从 0 开始）
	 *
	 * @return 零基页码
	 */
	public int toSpringPageNumber() {
		return normalizedPageNum() - 1;
	}
}
