package com.university.webdesign.config;

import com.university.webdesign.event.OrderWindowClosedEvent;
import com.university.webdesign.service.operation.DeliveryService;
import com.university.webdesign.service.operation.ServiceWindowService;
import com.university.webdesign.service.operation.dto.ServiceWindowVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 运营与履约系统的定时任务配置。
 * <p>
 * 对应《对外方法表》4.3：
 * <ul>
 *     <li>{@code OrderWindowCloseJob}：{@code 0 0 9 * * ?} 到达订餐截止时间，
 *         发布 {@link OrderWindowClosedEvent} 触发当日总括订单聚合；</li>
 *     <li>{@code DeliveryOpenJob}：{@code 0 30 11 * * ?} 到达配餐开始时间，
 *         调用 {@code DeliveryService.generateTasks} 预生成配送任务。</li>
 * </ul>
 * 两个任务都是**兜底 + 幂等**：真正的取值时间来自 `service_window` 配置
 * （{@code ServiceWindowService.get}），聚合与派单本身都可重复执行。
 * 单体单实例部署，无需分布式锁。
 */
@Slf4j
@EnableScheduling
@Component
public class OperationScheduleConfig
{
	private final ApplicationEventPublisher eventPublisher;
	private final ServiceWindowService serviceWindowService;
	private final DeliveryService deliveryService;

	public OperationScheduleConfig(
			ApplicationEventPublisher eventPublisher,
			ServiceWindowService serviceWindowService,
			DeliveryService deliveryService) {
		this.eventPublisher = eventPublisher;
		this.serviceWindowService = serviceWindowService;
		this.deliveryService = deliveryService;
	}

	/**
	 * 订餐窗口关闭任务：默认每天 09:00 发布 {@link OrderWindowClosedEvent}
	 * <p>
	 * 事件载荷带上数据库里当天生效的截止时间，便于日志与下游判断。
	 */
	@Scheduled(cron = "0 0 9 * * ?")
	public void orderWindowCloseJob() {
		LocalDate today = LocalDate.now();
		ServiceWindowVO window = serviceWindowService.get(today, null, null);
		LocalTime cutoff = window == null ? null : window.getCutoffTime();
		log.info("订餐窗口关闭任务触发：date={}，生效截止时间={}", today, cutoff);
		eventPublisher.publishEvent(new OrderWindowClosedEvent(today, cutoff));
	}

	/**
	 * 配送开放任务：默认每天 11:30 预生成当日配送任务
	 * <p>
	 * 未到配餐开始时间时 {@code generateTasks} 会抛 42201，这里捕获后只记日志，
	 * 不影响任务调度（配置改晚时等下一轮手动触发）。
	 */
	@Scheduled(cron = "0 30 11 * * ?")
	public void deliveryOpenJob() {
		LocalDate today = LocalDate.now();
		try {
			deliveryService.generateTasks(today, null, null);
			log.info("配送开放任务完成：date={}", today);
		} catch (RuntimeException exception) {
			log.warn("配送开放任务未生成任何任务：date={}，原因={}", today, exception.getMessage());
		}
	}
}
