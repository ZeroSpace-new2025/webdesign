package com.university.webdesign.api.menu;

import lombok.Data;

/**
 * 图片上传响应（M1-06）。
 * <p>
 * 对应《对外方法表》M1-06 的返回：{@code imageUrl} 与 {@code fileKey}。
 * 当前实现为本地磁盘存储，{@code fileKey} 即落盘文件名（{@code imageUrl} 去掉
 * {@code /uploads/} 前缀），未来接对象存储时语义不变。
 */
@Data
public class ImageUploadVO
{
	/**
	 * 可访问的图片地址，形如 {@code /uploads/xxx.png}
	 */
	private String imageUrl;

	/**
	 * 存储对象的键（本地存储为文件名）
	 */
	private String fileKey;

	/**
	 * 由图片地址构造响应
	 *
	 * @param imageUrl 可访问的图片地址
	 * @return 上传响应
	 */
	public static ImageUploadVO of(String imageUrl) {
		ImageUploadVO vo = new ImageUploadVO();
		vo.setImageUrl(imageUrl);
		vo.setFileKey(imageUrl == null ? null : imageUrl.substring(imageUrl.lastIndexOf('/') + 1));
		return vo;
	}
}
