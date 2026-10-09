package com.university.webdesign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置。
 * <p>
 * 对应《对外方法表》1.4 的身份传递约定：本项目**不使用** Spring Security 的表单登录与
 * Session 认证，认证由 JWT + {@link AuthInterceptor} 完成（见 {@code AuthService.verifyToken}）。
 * 因此这里只需：
 * <ul>
 *     <li>关闭 CSRF（纯 JSON API + 无状态 JWT，不使用 Cookie 会话）；</li>
 *     <li>放行全部请求，把“谁能访问什么”交给 {@code AuthInterceptor} 的 {@code @RequiresPerm}
 *         与 service 层的数据行级校验；</li>
 *     <li>关闭表单登录与 HTTP Basic，避免重定向到登录页打断 API 调用。</li>
 * </ul>
 * <p>
 * 重构前的 {@code DevSecurityConfig}（开发期临时放行）已被本类取代并删除。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig
{
	/**
	 * 安全过滤链
	 *
	 * @param http Spring Security 配置入口
	 * @return 过滤链
	 * @throws Exception 配置异常
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable())
				.logout(logout -> logout.disable());
		return http.build();
	}
}
