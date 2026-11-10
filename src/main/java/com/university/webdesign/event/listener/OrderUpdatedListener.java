package com.university.webdesign.event.listener;

import com.university.webdesign.event.OrderUpdatedEvent;
import com.university.webdesign.service.operation.BlanketOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单变更事件监听器（M3）。
 * <p>
 * 对应《对外方法表》4.3：订单修改/取消/作废后发布 {@link OrderUpdatedEvent}，
 * 本监听器重算当日 {@code daily_statistics} 快照，保证总括订单与配送需求不落后于订单变更。
 * <p>
 * 重算是幂等的（复用既有行、清空明细再重填）：当日尚无快照时会顺带生成一份，
 * 若此刻还没到订餐截止时间，这份快照会在窗口关闭时由 {@link OrderWindowClosedListener}
 * 强制覆盖重算，结果最终一致。
 */
@Slf4j
@Component
public class OrderUpdatedListener
{
	private final BlanketOrderService blanketOrderService;

	public OrderUpdatedListener(BlanketOrderService blanketOrderService) {
		this.blanketOrderService = blanketOrderService;
	}

	/**
	 * 重算当日汇总快照（幂等，覆盖写）
	 * <p>
	 * {@code REQUIRES_NEW} 必须有：{@code AFTER_COMMIT} 阶段原事务已提交但同步器仍挂在当前线程，
	 * 以 {@code REQUIRED} 进入会“加入”这个已完成的事务而不开新事务，
	 * JPA 侧抛 {@code TransactionRequiredException: No active transaction}，
	 * 异常被事务同步器吞掉并打成 ERROR 日志——快照实际上永远刷不出来。
	 *
	 * @param event 订单变更事件
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onOrderUpdated(OrderUpdatedEvent event) {
		if (event == null || event.orderDate() == null) {
			return;
		}
		log.info("订单 {} 发生变更（{}），重算 {} 的每日汇总快照",
				event.orderId(), event.changeType(), event.orderDate());
		blanketOrderService.refreshDailyStat(event.orderDate());
	}
}
