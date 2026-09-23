package com.university.webdesign;

import com.university.webdesign.support.StubServicesTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 应用上下文加载测试
 * <p>
 * 其他模块的 service 实现尚未提供，这里导入桩配置以保证上下文可启动。
 */
@SpringBootTest
@Import(StubServicesTestConfiguration.class)
class WebdesignApplicationTests {

	@Test
	void contextLoads() {
	}

}
