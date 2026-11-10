package com.university.webdesign.event.listener;

import com.university.webdesign.event.OrderWindowClosedEvent;
import com.university.webdesign.service.operation.BlanketOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订餐窗口关闭事件监听器（M3）。
 * <p>
 * 对应《对外方法表》4.3：定时任务 {@code OrderWindowCloseJob} 在到达订餐截止时间时发布
 * {@link OrderWindowClosedEvent}，本监听器据此聚合当日总括订单并写入 `daily_statistics`，
 * 供厨房备料与生产单打印读取。
 * <p>
 * 两个注解都是必须的：
 * <ul>
 *     <li>{@code fallbackExecution = true}：定时任务发布事件时**没有事务**，
 *         而 {@code @TransactionalEventListener} 默认在没有事务时**静默跳过**——
 *         不加这个开关，09:00 的定时聚合从来不会执行（事件发了，监听器不跑）；</li>
 *     <li>{@code @Transactional(REQUIRES_NEW)}：监听器自身要写库，需要一个独立事务；
 *         在上层有事务（{@code AFTER_COMMIT}）时也必须挂起外层、另开新事务，
 *         否则会“加入”已完成的事务并抛 {@code TransactionRequiredException: No active transaction}。</li>
 * </ul>
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
	 * 聚合当日总括订单
	 * <p>
	 * 这里必须传 {@code force = true}（覆盖重算），不能复用“已有快照直接返回”的幂等分支：
	 * 截止前订单变更（{@link OrderCreatedListener} / {@link OrderUpdatedListener}）已经写过一份快照，
	 * 若此处只做幂等复用，窗口关闭这一刻算出的生产量就可能不是完整结果。
	 * 覆盖重算本身也是幂等的（复用既有行、清空明细再重填），重复触发不会产生重复数据。
	 *
	 * @param event 订餐窗口关闭事件
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onOrderWindowClosed(OrderWindowClosedEvent event) {
		if (event == null || event.date() == null) {
			return;
		}
		log.info("订餐窗口已关闭（截止时间 {}），开始聚合 {} 的总括订单", event.cutoffTime(), event.date());
		blanketOrderService.aggregate(event.date(), true);
	}
}
