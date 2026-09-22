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
	 */
	@Column(name = "order_number", nullable = false)
	private Long orderNumber;
	/**
	 * 用户ID
	 */
	@Column(name = "user_id", nullable = false)
	private Long userId;

	/**
	 * 商品ID列表
	 */
	@OneToMany(cascade = CascadeType.ALL,mappedBy = "order",orphanRemoval = true)
	private List<OrderItem> itemIds = new ArrayList<>();
	
	/**
	 * 总金额
	 */
	@Column(name = "total", nullable = false)
	private BigDecimal total;
	
	/**
	 * 订单状态
	 */
	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private OrderStatus status;
	
	/**
	 * 订单日期
	 */
	@Column(name = "order_date", nullable = false)
	@JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP)
	private LocalDateTime orderDate;
}
