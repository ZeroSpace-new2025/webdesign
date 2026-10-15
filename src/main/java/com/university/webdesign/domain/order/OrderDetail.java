package com.university.webdesign.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单明细（需求原文的 {@code Order_Detail}）。
 * <p>
 * <b>数据快照：</b>菜名、分类、单位、单价都是**下单时刻**的值，冗余保存在这里。
 * 菜谱或菜单后续的修改、下架、删除都只影响未来菜单，不会影响历史订单——
 * 这是需求明确要求的数据保护机制，改菜谱逻辑时务必保住这些字段的语义。
 */
@Data
@Entity
@Table(name = "order_detail")
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetail
{
	/**
	 * 明细ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "detail_id")
	private Long detailId;

	/**
	 * 所属订单
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private OrderForm order;

	/**
	 * 菜品（菜谱）ID
	 */
	@Column(name = "recipe_id", nullable = false)
	private Long recipeId;

	/**
	 * 下单时的菜名快照
	 */
	@Column(name = "recipe_name", nullable = false, length = 120)
	private String recipeName;

	/**
	 * 下单时的分类快照（供生产单按分类汇总）
	 */
	@Column(name = "category", length = 60)
	private String category;

	/**
	 * 下单时的计量单位快照（份/两/个）
	 */
	@Column(name = "unit", length = 20)
	private String unit;

	/**
	 * 下单时的单价快照
	 */
	@Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
	private BigDecimal unitPrice;

	/**
	 * 数量
	 */
	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	/**
	 * 小计金额
	 */
	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount = BigDecimal.ZERO;

	/**
	 * 计算小计金额
	 *
	 * @return 单价 × 数量，任一为空时按 0 处理
	 */
	public BigDecimal subtotal() {
		if (unitPrice == null || quantity == null) {
			return BigDecimal.ZERO;
		}
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}
}
