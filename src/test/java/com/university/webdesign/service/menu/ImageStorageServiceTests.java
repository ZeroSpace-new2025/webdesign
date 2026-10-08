package com.university.webdesign.service.menu;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.service.impl.menu.ImageStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ImageStorageService} 单元测试（使用 JUnit {@code @TempDir}，不启动 Spring 上下文）。
 * <p>
 * 覆盖 M1-06 的图片校验：仅接受 jpg/png/webp/gif、超限拒绝，
 * 成功时落盘并返回 {@code /uploads/xxx.ext} 形式的可访问地址。
 */
class ImageStorageServiceTests
{
	@TempDir
	private Path tempDir;

	@Test
	@DisplayName("上传图片：合法 png 落盘并返回可访问地址")
	void storeShouldSaveFileAndReturnUrl() {
		ImageStorageService storage = new ImageStorageService(tempDir.toString(), 1024 * 1024);
		MockMultipartFile file = new MockMultipartFile("file", "dish.PNG", "image/png", new byte[] {1, 2, 3});

		String url = storage.store(file);

		assertThat(url).startsWith("/uploads/").endsWith(".png");
		assertThat(Files.exists(tempDir.resolve(url.substring("/uploads/".length())))).isTrue();
	}

	@Test
	@DisplayName("上传图片：不支持的格式抛 40001")
	void storeShouldRejectUnsupportedType() {
		ImageStorageService storage = new ImageStorageService(tempDir.toString(), 1024 * 1024);
		MockMultipartFile file = new MockMultipartFile("file", "dish.txt", "text/plain", new byte[] {1, 2, 3});

		assertThat(expectBusinessException(() -> storage.store(file)).getErrorCode())
				.isEqualTo(ErrorCode.PARAM_INVALID);
	}

	@Test
	@DisplayName("上传图片：空文件抛 40001")
	void storeShouldRejectEmptyFile() {
		ImageStorageService storage = new ImageStorageService(tempDir.toString(), 1024 * 1024);
		MockMultipartFile file = new MockMultipartFile("file", "dish.png", "image/png", new byte[0]);

		assertThat(expectBusinessException(() -> storage.store(file)).getErrorCode())
				.isEqualTo(ErrorCode.PARAM_INVALID);
	}

	@Test
	@DisplayName("上传图片：超过大小上限抛 40001")
	void storeShouldRejectOversizeFile() {
		ImageStorageService storage = new ImageStorageService(tempDir.toString(), 2);
		MockMultipartFile file = new MockMultipartFile("file", "dish.png", "image/png", new byte[] {1, 2, 3});

		assertThat(expectBusinessException(() -> storage.store(file)).getErrorCode())
				.isEqualTo(ErrorCode.PARAM_INVALID);
	}

	private static BusinessException expectBusinessException(Runnable action) {
		try {
			action.run();
		} catch (BusinessException e) {
			return e;
		} catch (RuntimeException e) {
			throw new AssertionError("期望 BusinessException，实际抛出：" + e, e);
		}
		throw new AssertionError("期望抛出 BusinessException，但没有抛出异常");
	}
}
