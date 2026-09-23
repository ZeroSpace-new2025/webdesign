package com.university.webdesign.ordertransaction.data;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单明细实体
 * <p>
 * 按需求要求，明细中冗余存储下单时刻的菜名、分类与单价（数据快照），
 * 这样菜谱/菜单后续的修改或删除都不会影响历史订单的数据一致性。
 */
@Entity
@Table(name = "order_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;
	
	/**
	 * 菜品（菜谱）ID
	 * <p>
	 * 只保存 ID，不建立跨模块外键关联：菜品数据的所有权属于菜品与菜单中心。
	 */
	@Column(name = "item_id", nullable = false)
	private Long itemId;
	
	/**
	 * 下单时的菜名快照
	 */
	@Column(name = "item_name", nullable = false)
	private String itemName;
	
	/**
	 * 下单时的菜品分类快照
	 * <p>
	 * 供运营与履约系统按分类汇总生产单使用。
	 */
	@Column(name = "category")
	private String category;
	
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
	 * 计算该明细的小计金额
	 *
	 * @return 单价 × 数量，单价为空时按 0 处理
	 */
	public BigDecimal subtotal() {
		if (unitPrice == null || quantity == null) {
			return BigDecimal.ZERO;
		}
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}
}
