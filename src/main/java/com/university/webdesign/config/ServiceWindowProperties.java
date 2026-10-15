package com.university.webdesign.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * 时间窗口兜底配置。
 * <p>
 * 对应《对外方法表》4.2 约定：{@code ServiceWindowService.get} 在数据库没有配置时
 * 回退默认 09:00 / 11:30。这两个默认值来自需求原文，集中在这里，避免散落在各模块代码中。
 * 运行期真正生效的值以 {@code service_window} 表为准。
 * <p>
 * 标注 {@code @Component} 才会被注册为 Bean：重构初期本类只有
 * {@code @ConfigurationProperties}，全仓没有 {@code @EnableConfigurationProperties} 或
 * {@code @ConfigurationPropertiesScan}，导致配置绑定实际不生效（服务实现只能退化成内部默认值）。
 */
@Component
@ConfigurationProperties(prefix = "app.service-window")
public class ServiceWindowProperties
{
	/**
	 * 订餐截止时间，默认 09:00
	 */
	private LocalTime defaultCutoffTime = LocalTime.of(9, 0);

	/**
	 * 配餐开始时间，默认 11:30
	 */
	private LocalTime defaultDeliveryStartTime = LocalTime.of(11, 30);

	/**
	 * @return 订餐截止时间
	 */
	public LocalTime getDefaultCutoffTime() {
		return defaultCutoffTime;
	}

	/**
	 * @param defaultCutoffTime 订餐截止时间
	 */
	public void setDefaultCutoffTime(LocalTime defaultCutoffTime) {
		this.defaultCutoffTime = defaultCutoffTime;
	}

	/**
	 * @return 配餐开始时间
	 */
	public LocalTime getDefaultDeliveryStartTime() {
		return defaultDeliveryStartTime;
	}

	/**
	 * @param defaultDeliveryStartTime 配餐开始时间
	 */
	public void setDefaultDeliveryStartTime(LocalTime defaultDeliveryStartTime) {
		this.defaultDeliveryStartTime = defaultDeliveryStartTime;
	}
}
