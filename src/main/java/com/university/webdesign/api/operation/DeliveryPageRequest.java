package com.university.webdesign.api.operation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.university.webdesign.service.operation.dto.DeliveryQuery;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 配送任务分页查询入参。
 * <p>
 * 对应《对外方法表》M3-08：按日期/工位/状态/员工筛选。页面参数名按 M3 之前的既有写法
 * （`page`/`size`/`workstation`）绑定，内部再转换为统一的 {@link DeliveryQuery} 分页语义。
 * 原始分页字段不参与 JSON 序列化，避免与 {@code page}/{@code size} 重复输出。
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DeliveryPageRequest extends DeliveryQuery
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
