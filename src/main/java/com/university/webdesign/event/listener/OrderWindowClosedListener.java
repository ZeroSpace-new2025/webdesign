package com.university.webdesign.event.listener;

import com.university.webdesign.event.OrderWindowClosedEvent;
import com.university.webdesign.service.operation.BlanketOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订餐窗口关闭事件监听器（M3）。
 * <p>
 * 对应《对外方法表》4.3：定时任务 {@code OrderWindowCloseJob} 在到达订餐截止时间时发布
 * {@link OrderWindowClosedEvent}，本监听器据此聚合当日总括订单并写入 `daily_statistics`，
 * 供厨房备料与生产单打印读取。
 * <p>
 * 用 {@code AFTER_COMMIT} 监听：订单事务（如有）提交后再聚合，避免读到未提交数据；
 * 事件来源没有事务时（定时任务场景）同样会执行。
 */
@Slf4j
@Component
public class OrderWindowClosedListener
{
	private final BlanketOrderService blanketOrderService;

	public OrderWindowClosedListener(BlanketOrderService blanketOrderService) {
		this.blanketOrderService = blanketOrderService;
	}

	/**
	 * 聚合当日总括订单（幂等：已有快照直接复用）
	 *
	 * @param event 订餐窗口关闭事件
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onOrderWindowClosed(OrderWindowClosedEvent event) {
		if (event == null || event.date() == null) {
			return;
		}
		log.info("订餐窗口已关闭（截止时间 {}），开始聚合 {} 的总括订单", event.cutoffTime(), event.date());
		blanketOrderService.aggregate(event.date(), false);
	}
}
