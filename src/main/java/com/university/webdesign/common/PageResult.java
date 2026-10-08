package com.university.webdesign.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 统一分页返回体。
 * <p>
 * 对应《对外方法表》1.4 通用约定：{@code PageResult<T>{total, list}}。
 *
 * @param <T> 列表元素类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T>
{
	/**
	 * 总记录数
	 */
	private long total;

	/**
	 * 当前页数据
	 */
	private List<T> list;

	/**
	 * 空结果
	 *
	 * @param <T> 元素类型
	 * @return 空分页结果
	 */
	public static <T> PageResult<T> empty() {
		return new PageResult<>(0L, List.of());
	}

	/**
	 * 由完整列表构造（不分页场景：全部数据放在当前页）
	 *
	 * @param list 全部数据
	 * @param <T>  元素类型
	 * @return 分页结果
	 */
	public static <T> PageResult<T> of(List<T> list) {
		List<T> safe = list == null ? List.of() : list;
		return new PageResult<>(safe.size(), safe);
	}

	/**
	 * 由 Spring Data 分页对象构造，并逐条转换元素类型
	 *
	 * @param page       Spring Data 分页结果
	 * @param converter  元素转换函数
	 * @param <E>        实体类型
	 * @param <T>        目标类型
	 * @return 统一分页结果
	 */
	public static <E, T> PageResult<T> from(Page<E> page, Function<E, T> converter) {
		return new PageResult<>(page.getTotalElements(),
				page.getContent().stream().map(converter).toList());
	}

	/**
	 * 由完整列表构造分页结果（内存分页）
	 *
	 * @param all      全部数据
	 * @param pageNum  页码，从 1 开始
	 * @param pageSize 每页条数
	 * @param <T>      元素类型
	 * @return 分页结果
	 */
	public static <T> PageResult<T> ofPage(List<T> all, int pageNum, int pageSize) {
		List<T> safe = all == null ? List.of() : all;
		int from = Math.max(0, (pageNum - 1) * pageSize);
		if (from >= safe.size()) {
			return new PageResult<>(safe.size(), List.of());
		}
		int to = Math.min(safe.size(), from + pageSize);
		return new PageResult<>(safe.size(), List.copyOf(safe.subList(from, to)));
	}
}
