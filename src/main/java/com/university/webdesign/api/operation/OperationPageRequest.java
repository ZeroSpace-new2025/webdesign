package com.university.webdesign.api.operation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.university.webdesign.common.PageQuery;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 运营模块统一的分页查询入参。
 * <p>
 * 对应《对外方法表》1.4 的分页约定（`PageResult<T>{total, list}`，`pageSize` 上限 100）。
 * 页面与前端脚本沿用 M3 既有的 `page`/`size` 参数名，这里做一层绑定与归一化，
 * 业务层拿到的仍是 {@link PageQuery} 语义（{@code pageNum} 从 1 开始）。
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class OperationPageRequest extends PageQuery
{
	/**
	 * 页码，从 1 开始，默认 1
	 */
	private Integer page;

	/**
	 * 每页条数，上限 100，默认 20
	 */
	private Integer size;

	@Override
	@JsonIgnore
	public Integer getPageNum() {
		return page == null || page < 1 ? 1 : page;
	}

	@Override
	@JsonIgnore
	public Integer getPageSize() {
		if (size == null || size < 1) {
			return DEFAULT_PAGE_SIZE;
		}
		return Math.min(size, MAX_PAGE_SIZE);
	}
}
