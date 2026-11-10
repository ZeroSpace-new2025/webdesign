package com.university.webdesign.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web 配置。
 * <ul>
 *     <li>把 {@code /uploads/**} 映射到本地图片上传目录，使上传的菜品图片可被访问；</li>
 *     <li>注册 {@link AuthInterceptor}，并对登录页、静态资源、H2 控制台开放白名单。</li>
 * </ul>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer
{
	/**
	 * 免认证路径白名单：登录接口、错误页、静态资源、上传文件、H2 控制台
	 */
	private static final String[] AUTH_WHITELIST = {
			"/api/v1/user/auth/login",
			"/error",
			"/css/**",
			"/js/**",
			"/images/**",
			"/uploads/**",
			"/favicon.ico",
			"/h2-console/**"
	};

	private final AuthInterceptor authInterceptor;

	/**
	 * 导航栏权限拦截器：视图渲染前注入当前账号的权限点与角色
	 */
	private final NavPermissionInterceptor navPermissionInterceptor;

	@Value("${app.upload.dir:uploads}")
	private String uploadDir;

	public WebConfig(AuthInterceptor authInterceptor, NavPermissionInterceptor navPermissionInterceptor) {
		this.authInterceptor = authInterceptor;
		this.navPermissionInterceptor = navPermissionInterceptor;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations("file:" + uploadPath + "/");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(authInterceptor)
				.addPathPatterns("/**")
				.excludePathPatterns(AUTH_WHITELIST);
		// 放在认证之后：登录态已经在 preHandle 里写好，导航拦截器只需在渲染前补模型属性
		registry.addInterceptor(navPermissionInterceptor)
				.addPathPatterns("/**")
				.excludePathPatterns(AUTH_WHITELIST);
	}
}
