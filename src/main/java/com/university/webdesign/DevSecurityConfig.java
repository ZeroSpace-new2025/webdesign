package com.university.webdesign;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 开发期安全放行配置（临时）
 * <p>
 * 背景：项目已引入 {@code spring-boot-starter-security}，但登录认证属于用户与报表中心（IAM）的职责，
 * 目前尚未落地。没有本配置时，默认安全过滤链会拦截全部请求（含 Thymeleaf 页面），
 * 导致订单模块的页面无法打开；因此这里先提供一条“全部放行”的过滤链。
 * <p>
 * <b>本类只用于让各模块的页面在登录态接入前可以独立开发调试，不是最终安全方案。</b>
 * <ul>
 *     <li>不做任何身份校验，任何人可访问任意页面与接口；</li>
 *     <li>关闭 CSRF：当前没有基于会话的登录态，CSRF 没有要保护的凭据，
 *         且页面表单在无登录态下提交会被令牌校验挡住，反而影响联调；</li>
 *     <li>关闭表单登录与 HTTP Basic，避免启动时生成随机密码造成误解。</li>
 * </ul>
 * <p>
 * //todo 确认：用户与报表中心（王家豪）接入 Spring Security 登录/角色后，
 * 应删除本类，改为按角色授权（员工只能访问点餐与“我的订单”，经理可访问删单入口等），
 * 并同时恢复 CSRF 防护。订单模块的 {@code operatorId} 也应随之改为从认证上下文获取
 * （见 {@code OrderCreateData#getOperatorId()} 上的说明）。
 */
@Configuration
public class DevSecurityConfig
{
	/**
	 * 开发期过滤链：放行全部请求
	 *
	 * @param http Spring Security 的 HTTP 配置入口
	 * @return 过滤链
	 * @throws Exception 配置异常
	 */
	@Bean
	public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
				.csrf(csrf -> csrf.disable())
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable())
				.logout(logout -> logout.disable());
		return http.build();
	}
}
