package com.university.webdesign.event.listener;

import com.university.webdesign.event.OrderUpdatedEvent;
import com.university.webdesign.service.operation.BlanketOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单变更事件监听器（M3）。
 * <p>
 * 对应《对外方法表》4.3：订单修改/取消/作废后发布 {@link OrderUpdatedEvent}，
 * 本监听器重算当日 {@code daily_statistics} 快照，保证总括订单与配送需求不落后于订单变更。
 * <p>
 * 重算是幂等的（先删旧快照再写新快照）：当日尚无快照时会顺带生成一份，
 * 若此刻还没到订餐截止时间，这份快照会在窗口关闭时由定时聚合再次覆盖，结果最终一致。
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
	 *
	 * @param event 订单变更事件
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onOrderUpdated(OrderUpdatedEvent event) {
		if (event == null || event.orderDate() == null) {
			return;
		}
		log.info("订单 {} 发生变更（{}），重算 {} 的每日汇总快照",
				event.orderId(), event.changeType(), event.orderDate());
		blanketOrderService.refreshDailyStat(event.orderDate());
	}
}
