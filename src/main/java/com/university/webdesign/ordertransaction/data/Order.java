package com.university.webdesign.ordertransaction.data;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单主表实体
 * <p>
 * 一个员工同一天最多只能有一张有效订单（“一人一天一单”），该规则由
 * {@code com.university.webdesign.ordertransaction.impl.OrderServiceImpl} 在服务层校验。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "my_order")
public class Order
{
	/**
	 * 订单ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	/**
	 * 订单号
	 * <p>
	 * 对外展示与打印使用的业务单号，规则为 yyMMdd + 5 位当日流水。
	 */
	@Column(name = "order_number", nullable = false, unique = true)
	private Long orderNumber;
	
	/**
	 * 用户ID
	 * <p>
	 * 只保存 ID，不建立跨模块外键关联：员工基础数据的唯一所有者是用户与报表中心。
	 */
	@Column(name = "user_id", nullable = false)
	private Long userId;
	
	/**
	 * 订单明细列表
	 */
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "order", orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();
	
	/**
	 * 总金额
	 */
	@Column(name = "total", nullable = false, precision = 12, scale = 2)
	private BigDecimal total;
	
	/**
	 * 订单状态
	 */
	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private OrderStatus status;
	
	/**
	 * 订单日期（下单时间）
	 */
	@Column(name = "order_date", nullable = false)
	@JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP)
	private LocalDateTime orderDate;
	
	/**
	 * 最后修改时间
	 * <p>
	 * 员工改单、经理删单前更新该字段。
	 */
	@Column(name = "last_modified_time")
	@JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP)
	private LocalDateTime lastModifiedTime;
	
	/**
	 * 添加一条明细并维护双向关联
	 *
	 * @param item 订单明细
	 */
	public void addItem(OrderItem item) {
		item.setOrder(this);
		this.items.add(item);
	}
	
	/**
	 * 清空明细（用于改单时整体替换）
	 */
	public void clearItems() {
		this.items.clear();
	}
	
	/**
	 * 按当前明细重算总金额
	 *
	 * @return 重算后的总金额
	 */
	public BigDecimal recalculateTotal() {
		BigDecimal sum = BigDecimal.ZERO;
		for (OrderItem item : this.items) {
			sum = sum.add(item.subtotal());
		}
		this.total = sum;
		return sum;
	}
}
