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

	@Value("${app.upload.dir:uploads}")
	private String uploadDir;

	public WebConfig(AuthInterceptor authInterceptor) {
		this.authInterceptor = authInterceptor;
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
	}
}
