package com.university.webdesign.service.impl.menu;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 菜品图片本地存储服务（M1-06 的落盘实现）。
 * <p>
 * 课程项目用本地磁盘模拟对象存储：文件写入 {@code app.upload.dir}（默认 {@code uploads}），
 * 由 {@code WebConfig} 把 {@code /uploads/**} 映射到该目录，因此返回的相对地址
 * {@code /uploads/xxx.png} 可直接访问。
 * <p>
 * 校验规则：只接受 jpg/jpeg/png/webp/gif；大小不超过 {@code app.upload.max-size}
 * （默认 5MB，字节）；文件名用 UUID 重写，只保留白名单扩展名，避免路径穿越与重名覆盖。
 * 任何失败（空文件/类型不支持/超限/读写异常）都抛 40001，图片属于入参。
 */
@Service
public class ImageStorageService
{
	/**
	 * 允许的图片扩展名
	 */
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

	/**
	 * 允许的图片 MIME 类型
	 */
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

	/**
	 * 返回地址前缀，与 WebConfig 的静态资源映射保持一致
	 */
	private static final String URL_PREFIX = "/uploads/";

	/**
	 * 图片存储根目录（绝对路径）
	 */
	private final Path uploadRoot;

	/**
	 * 允许的最大字节数
	 */
	private final long maxSize;

	/**
	 * @param uploadDir 存储目录，取配置 {@code app.upload.dir}，默认 {@code uploads}
	 * @param maxSize   单文件上限（字节），取配置 {@code app.upload.max-size}，默认 5MB
	 */
	public ImageStorageService(
			@Value("${app.upload.dir:uploads}") String uploadDir,
			@Value("${app.upload.max-size:5242880}") long maxSize) {
		this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
		this.maxSize = maxSize;
	}

	/**
	 * 保存图片并返回可访问地址
	 *
	 * @param file 上传的图片文件
	 * @return 形如 {@code /uploads/xxx.png} 的可访问地址
	 */
	public String store(MultipartFile file) {
		validate(file);
		String extension = resolveExtension(file);
		String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
		try {
			Files.createDirectories(this.uploadRoot);
			Path target = this.uploadRoot.resolve(fileName).normalize();
			if (!target.startsWith(this.uploadRoot)) {
				throw new BusinessException(ErrorCode.PARAM_INVALID, "非法的图片文件名：" + fileName);
			}
			Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
			return URL_PREFIX + fileName;
		} catch (IOException e) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "图片保存失败：" + fileName, e);
		}
	}

	/**
	 * 基础校验：非空、大小、MIME 类型
	 */
	private void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "上传的图片文件为空");
		}
		if (this.maxSize > 0 && file.getSize() > this.maxSize) {
			throw new BusinessException(ErrorCode.PARAM_INVALID,
					"图片大小不能超过 " + (this.maxSize / 1024 / 1024) + "MB");
		}
		String contentType = file.getContentType();
		if (contentType != null && !contentType.isBlank()
				&& !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "仅支持 jpg/png/webp/gif 格式的图片");
		}
	}

	/**
	 * 取扩展名：优先用原文件名（只保留白名单后缀），文件名无后缀时按 MIME 类型推导
	 */
	private String resolveExtension(MultipartFile file) {
		String extension = null;
		String original = file.getOriginalFilename();
		if (original != null) {
			int dot = original.lastIndexOf('.');
			if (dot >= 0 && dot < original.length() - 1) {
				extension = original.substring(dot + 1).toLowerCase(Locale.ROOT);
			}
		}
		if (extension == null || extension.isBlank()) {
			String contentType = file.getContentType();
			if (contentType != null) {
				extension = switch (contentType.toLowerCase(Locale.ROOT)) {
					case "image/jpeg" -> "jpg";
					case "image/png" -> "png";
					case "image/webp" -> "webp";
					case "image/gif" -> "gif";
					default -> null;
				};
			}
		}
		if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "仅支持 jpg/png/webp/gif 格式的图片");
		}
		return "jpeg".equals(extension) ? "jpg" : extension;
	}
}
