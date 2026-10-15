package com.university.webdesign.service.menu;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.service.menu.dto.CategoryVO;
import com.university.webdesign.service.menu.dto.RecipeCreateCmd;
import com.university.webdesign.service.menu.dto.RecipeQuery;
import com.university.webdesign.service.menu.dto.RecipeUpdateCmd;
import com.university.webdesign.service.menu.dto.RecipeVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 菜品（食谱）服务契约——M1 对外能力之一。
 * <p>
 * 对应《对外方法表》2.2 的 {@code RecipeService}（7 个方法），api 层
 * {@code ApiRecipeController} 与 M3 生产单分类统计都只依赖本接口。
 * <p>
 * 约定：
 * <ul>
 *     <li>业务失败一律抛 {@code BusinessException}：重名 40901、不存在 40400、
 *         被已发布菜单引用时下架 42203、参数非法 40001、乐观锁冲突 40902；</li>
 *     <li>操作人身份取自 {@code UserContextHolder.currentUserId()}，接口不接收 operatorId；</li>
 *     <li>事务统一用普通 {@code @Transactional}，查询也不加 {@code readOnly = true}。</li>
 * </ul>
 */
public interface RecipeService
{
	/**
	 * 新增菜品（M1-01）
	 * <p>
	 * 校验菜品名唯一（重复抛 40901）与单价合法性，落库并发布
	 * {@code RecipeChangedEvent(CREATED)}。
	 *
	 * @param cmd 新增请求
	 * @return 新菜品ID
	 */
	Long create(RecipeCreateCmd cmd);

	/**
	 * 更新菜品（M1-02）
	 * <p>
	 * 只改 {@code recipe} 表本体，**不触碰任何快照与历史订单**；
	 * 入参 {@code version} 与库中不一致时抛 40902，改名撞车抛 40901。
	 *
	 * @param recipeId 菜品ID
	 * @param cmd      更新请求
	 */
	void update(Long recipeId, RecipeUpdateCmd cmd);

	/**
	 * 逻辑下架菜品（M1-03）
	 * <p>
	 * 置 {@code status=DISABLED}，不做物理删除以保护历史数据；
	 * 若被“已发布（PUBLISHED）菜单”引用则抛 42203 并提示先下架菜单。
	 *
	 * @param recipeId 菜品ID
	 * @param reason   下架原因，可为空
	 * @return 受影响菜单数（仍可编辑的 DRAFT 菜单数；已发布菜单已冻结快照、历史菜单不受影响）
	 */
	int disable(Long recipeId, String reason);

	/**
	 * 查询菜品详情（M1-04）
	 *
	 * @param recipeId 菜品ID
	 * @return 菜品视图
	 */
	RecipeVO getById(Long recipeId);

	/**
	 * 分页查询菜品（M1-05）
	 *
	 * @param q 查询条件（名称模糊、分类、状态、价格区间 + 分页）
	 * @return 分页菜品
	 */
	PageResult<RecipeVO> page(RecipeQuery q);

	/**
	 * 上传菜品图片（M1-06）
	 * <p>
	 * 校验图片类型（jpg/png/webp/gif）与大小，存本地 {@code app.upload.dir}
	 * （默认 {@code uploads}）并返回可访问地址（形如 {@code /uploads/xxx.png}）；
	 * 类型/大小/存储失败均按 40001 返回。
	 *
	 * @param file 上传文件（multipart/form-data）
	 * @return 可访问的图片地址
	 */
	String uploadImage(MultipartFile file);

	/**
	 * 查询菜品分类字典（M1-07）
	 * <p>
	 * 当前由 {@code recipe.category} 去重聚合（忽略空值）得到，
	 * {@code categoryId} 取分类名的稳定哈希；未来可独立建分类表。
	 *
	 * @return 分类列表（按名称升序）
	 */
	List<CategoryVO> listCategories();
}
