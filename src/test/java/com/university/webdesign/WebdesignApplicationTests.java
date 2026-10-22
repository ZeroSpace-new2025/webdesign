package com.university.webdesign;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 应用上下文加载测试。
 * <p>
 * 四个模块的 service 实现均已落地，上下文不再需要桩配置；本测试只验证
 * Spring 上下文（数据源、安全、拦截器、定时任务、各模块 Bean）能够正常装配。
 */
@SpringBootTest
class WebdesignApplicationTests
{
	@Test
	void contextLoads() {
	}
}
