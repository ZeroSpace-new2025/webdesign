package com.university.webdesign.service.order.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 改单请求。
 * <p>
 * 对应《对外方法表》M2-03：仅本人、仅 `PENDING`、未过截止时间；重写明细快照并重算总价。
 * `version` 用于乐观锁，不一致时抛 40902。
 */
@Data
public class OrderModifyCmd
{
	/**
	 * 明细列表（整体替换，不允许为空）
	 */
	@NotEmpty(message = "请至少选择一道菜品")
	private List<OrderSubmitCmd.SubmitItem> items;

	/**
	 * 备注
	 */
	private String remark;

	/**
	 * 乐观锁版本号，可为空表示不校验
	 */
	private Integer version;
}
