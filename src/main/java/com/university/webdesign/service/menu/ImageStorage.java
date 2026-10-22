package com.university.webdesign.service.menu;

import org.springframework.web.multipart.MultipartFile;

/**
 * 菜品图片存储契约（M1-06 “图片上传服务”）。
 * <p>
 * demand.md 要求图片上传“对接对象存储”，因此这里把“存哪儿”抽成接口：
 * <ul>
 *     <li>开发/演示环境用本地磁盘实现（{@code app.upload.mode=local}，默认），
 *         文件写到 {@code app.upload.dir} 并由 {@code /uploads/**} 静态映射对外；</li>
 *     <li>生产环境切换为对象存储时，只需新增一个实现本接口的 {@code @Service}
 *         （如阿里云 OSS / MinIO / S3），把 {@code app.upload.mode} 改成对应值即可，
 *         业务层（{@link RecipeService#uploadImage}）与页面无需改动。</li>
 * </ul>
 * 实现方自行负责校验（类型/大小）与命名（避免覆盖与路径穿越），失败时抛
 * {@code BusinessException(ErrorCode.PARAM_INVALID, "中文提示")}。
 */
public interface ImageStorage
{
	/**
	 * 保存图片并返回可直接访问的地址
	 *
	 * @param file 上传的图片文件（multipart/form-data）
	 * @return 图片地址：本地实现返回 {@code /uploads/xxx.png}；对象存储实现返回其公网/内网 URL
	 */
	String store(MultipartFile file);
}
