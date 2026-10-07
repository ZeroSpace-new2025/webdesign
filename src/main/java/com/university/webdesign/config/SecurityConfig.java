package com.university.webdesign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置。
 * 当前阶段为便于前后端联调，放开所有接口与静态资源的访问，并关闭 CSRF。
 * 待用户与权限模块（User Service）完善后，应在此接入基于角色的访问控制。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable());
		return http.build();
	}
}
