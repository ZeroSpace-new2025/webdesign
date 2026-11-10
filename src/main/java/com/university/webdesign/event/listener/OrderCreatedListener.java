package com.university.webdesign.event.listener;

import com.university.webdesign.event.OrderCreatedEvent;
import com.university.webdesign.service.operation.BlanketOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单创建事件监听器（M3）。
 * <p>
 * 对应《对外方法表》4.3：下单成功后发布 {@link OrderCreatedEvent}，
 * 本监听器重算当日 {@code daily_statistics} 快照，保证总括订单与配送需求不落后于新增订单。
 * <p>
 * **为什么必须有这个监听器**：此前只有 {@link OrderUpdatedListener}（改单/取消/作废）会重算快照，
 * 新建订单不会，导致当日快照停在“上一次变更”的时刻。而
 * {@code BlanketOrderService.aggregate(date, force=false)} 是幂等的（已有快照直接复用），
 * 09:00 的定时聚合也不会覆盖它，于是厨房看到的生产数量会**少算截止前新提交的订单**。
 * <p>
 * 重算是幂等的（复用既有行、清空明细再重填），先下单后截止时这份快照也会在窗口关闭时被
 * {@link OrderWindowClosedListener} 再覆盖一次，结果最终一致。
 */
@Slf4j
@Component
public class OrderCreatedListener
{
	private final BlanketOrderService blanketOrderService;

	public OrderCreatedListener(BlanketOrderService blanketOrderService) {
		this.blanketOrderService = blanketOrderService;
	}

	/**
	 * 重算当日汇总快照（幂等，覆盖写）
	 * <p>
	 * {@code REQUIRES_NEW} 不是可选项：{@code AFTER_COMMIT} 阶段原事务已经提交、但同步器仍挂在
	 * 线程上（{@code TransactionSynchronizationManager} 还认为事务“存在”），此时以 {@code REQUIRED}
	 * 进入会“加入”这个已完成的事务而不开新事务，JPA 侧就报
	 * {@code TransactionRequiredException: No active transaction}——异常被
	 * {@code TransactionSynchronizationUtils} 吞掉并打成 ERROR 日志，快照永远刷不出来。
	 * 挂起外层、单独开一个新事务后，重算才能正常落库。
	 *
	 * @param event 订单创建事件
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onOrderCreated(OrderCreatedEvent event) {
		if (event == null || event.orderDate() == null) {
			return;
		}
		log.info("订单 {} 已创建，重算 {} 的每日汇总快照", event.orderId(), event.orderDate());
		blanketOrderService.refreshDailyStat(event.orderDate());
	}
}
