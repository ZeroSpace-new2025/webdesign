package com.university.webdesign.config;

import com.university.webdesign.common.AuthSession;
import com.university.webdesign.user.api.AuthenticatedUser;
import com.university.webdesign.user.service.UserAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 系统安全配置。
 * <p>
 * 用户与报表中心的登录页负责收集凭据，认证与授权统一交给 Spring Security：
 * <ul>
 *     <li>表单登录：GET /login 渲染页面，POST /login 校验用户名和密码；</li>
 *     <li>角色授权：经理/财务可进入用户与报表控制台，员工进入点餐页面；</li>
 *     <li>CSRF：恢复默认防护，页面表单与前端接口统一携带令牌；</li>
 *     <li>会话桥接：登录成功后写入 AuthSession，兼容订单模块现有的 operatorId 读取方式。</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig
{
	/**
	 * 用户密码使用 BCrypt 存储，数据库种子数据和用户服务共用同一个编码器。
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * 应用安全过滤链。
	 *
	 * @param http                       Spring Security 配置入口
	 * @param userAuthenticationProvider 用户中心提供的认证适配器
	 * @return 过滤链
	 * @throws Exception 配置异常
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		UserAuthenticationProvider userAuthenticationProvider) throws Exception {
		http
			.authenticationProvider(userAuthenticationProvider)
			.authorizeHttpRequests(auth -> auth
				// 登录页、静态资源和错误页不需要登录
				.requestMatchers("/login", "/error", "/favicon.ico", "/css/**", "/js/**", "/images/**", "/uploads/**")
					.permitAll()
				// 当前用户信息与修改自己的密码：任何已登录用户可访问
				.requestMatchers(HttpMethod.GET, "/api/user/me").authenticated()
				.requestMatchers(HttpMethod.POST, "/api/user/change-password").authenticated()
				// 角色和用户读取：经理、财务可以查看；其余写操作仅经理
				.requestMatchers(HttpMethod.GET, "/api/role", "/api/role/permission")
					.hasAnyRole("MANAGER", "FINANCE")
				.requestMatchers(HttpMethod.POST, "/api/user/query")
					.hasAnyRole("MANAGER", "FINANCE")
				.requestMatchers(HttpMethod.GET, "/api/user/*")
					.hasAnyRole("MANAGER", "FINANCE")
				.requestMatchers("/api/user/**", "/api/role/**")
					.hasRole("MANAGER")
				// 月度销售报表：经理、财务、厨房主管可看；消费审计和刷新仅经理、财务
				.requestMatchers("/api/report/employees", "/api/report/employee/**")
					.hasAnyRole("MANAGER", "FINANCE")
				.requestMatchers(HttpMethod.POST, "/api/report/refresh")
					.hasAnyRole("MANAGER", "FINANCE")
				.requestMatchers(HttpMethod.GET, "/api/report/**")
					.hasAnyRole("MANAGER", "FINANCE", "KITCHEN_SUPERVISOR")
				// 控制台：用户与报表中心的首页
				.requestMatchers("/console")
					.hasAnyRole("MANAGER", "FINANCE")
				// 菜品与菜单中心
				.requestMatchers("/recipes/**", "/menus/**", "/api/recipe/**", "/api/menu/**")
					.hasAnyRole("MANAGER", "KITCHEN_SUPERVISOR")
				// 运营与履约系统
				.requestMatchers("/api/fulfillment/**")
					.hasAnyRole("MANAGER", "KITCHEN_SUPERVISOR", "DELIVERY_STAFF")
				// 点餐与个人订单：所有已登录员工都可访问
				.requestMatchers("/order/**", "/api/order/**")
					.authenticated()
				.anyRequest().authenticated())
			.formLogin(form -> form
				.loginPage("/login")
				.loginProcessingUrl("/login")
				.usernameParameter("username")
				.passwordParameter("password")
				.successHandler((request, response, authentication) -> {
					if (authentication.getPrincipal() instanceof AuthenticatedUser user) {
						request.getSession(true).setAttribute(AuthSession.USER_ID, user.userId());
						// 兼容订单模块尚未迁移到认证上下文的会话约定
						request.getSession().setAttribute(AuthSession.ORDER_OPERATOR_ID, user.userId());
					}
					response.sendRedirect("/");
				})
				.failureUrl("/login?error")
				.permitAll())
			.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessUrl("/login?logout")
				.invalidateHttpSession(true)
				.clearAuthentication(true)
				.deleteCookies("JSESSIONID")
				.permitAll())
			.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
			.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
		return http.build();
	}
}
