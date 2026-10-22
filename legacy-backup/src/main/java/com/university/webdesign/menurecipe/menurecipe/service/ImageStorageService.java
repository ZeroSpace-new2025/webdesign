package com.university.webdesign.menurecipe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 图片存储服务。
 * 课程项目中采用本地文件存储（模拟对接对象存储），上传文件保存到配置目录，
 * 通过 /uploads/** 静态资源路径对外访问。
 */
@Service
public class ImageStorageService {

	private final Path uploadRoot;

	public ImageStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
		this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
		try {
			Files.createDirectories(this.uploadRoot);
		} catch (IOException e) {
			throw new IllegalStateException("无法创建上传目录: " + this.uploadRoot, e);
		}
	}

	/**
	 * 保存图片，返回可访问的相对地址，如 /uploads/xxx.jpg
	 */
	public String store(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("上传的图片文件为空");
		}
		String original = file.getOriginalFilename();
		String ext = "";
		if (original != null && original.contains(".")) {
			ext = original.substring(original.lastIndexOf('.'));
		}
		String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
		try {
			Path target = this.uploadRoot.resolve(fileName).normalize();
			Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
			return "/uploads/" + fileName;
		} catch (IOException e) {
			throw new IllegalStateException("图片保存失败: " + fileName, e);
		}
	}
}
