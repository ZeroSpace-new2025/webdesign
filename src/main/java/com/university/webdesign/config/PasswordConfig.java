package com.university.webdesign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置。
 * <p>
 * 全模块共用同一个 {@link PasswordEncoder}：用户中心用它比对登录密码与初始化新账号密码；
 * 重构前该 Bean 缺失会导致 {@code JpaUserService} 无法装配，这里统一补齐。
 */
@Configuration
public class PasswordConfig
{
	/**
	 * BCrypt 密码编码器
	 *
	 * @return 密码编码器
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
