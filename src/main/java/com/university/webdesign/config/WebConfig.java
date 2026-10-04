package com.university.webdesign.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web 配置。
 * 将 /uploads/** 映射到本地图片上传目录，使上传的菜品图片可被访问。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${app.upload.dir:uploads}")
	private String uploadDir;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations("file:" + uploadPath + "/");
	}
}
