package com.university.webdesign.service.menu;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.service.menu.dto.MenuCreateCmd;
import com.university.webdesign.service.menu.dto.MenuQuery;
import com.university.webdesign.service.menu.dto.MenuUpdateCmd;
import com.university.webdesign.service.menu.dto.MenuVO;
import com.university.webdesign.service.menu.dto.PublishResultVO;

import java.time.LocalDate;

/**
 * 菜单服务契约——M1 对外能力之一。
 * <p>
 * 对应《对外方法表》2.2 的 {@code MenuService}（6 个方法）。
 * 其中 {@link #getCurrent(LocalDate)} 是**跨模块主入口**：M2 订单核心下单选菜、
 * 校验菜品是否在菜单内、取价全部走它（见《重构实施规范》第 6 节）。
 * <p>
 * 关键不变量：
 * <ul>
 *     <li>仅 {@code DRAFT} 可改，每次改动 {@code version} 自增并返回新版本号；</li>
 *     <li>发布时冻结快照并置 {@code locked=true}，此后不可改不可删，只能下架；</li>
 *     <li>业务失败抛 {@code BusinessException}：不存在 40400、非草稿被改 40902、
 *         乐观锁冲突 40902、参数非法 40001。</li>
 * </ul>
 */
public interface MenuService
{
	/**
	 * 创建菜单草稿（M1-08）
	 * <p>
	 * 合并写入 {@code menu} 与 {@code menu_item}；菜单级价格为空时取菜品标准单价，
	 * 菜名/分类/单位/图片从菜品复制成菜单项副本。
	 *
	 * @param cmd 创建请求
	 * @return 新菜单ID
	 */
	Long createDraft(MenuCreateCmd cmd);

	/**
	 * 更新草稿菜单（M1-09）
	 * <p>
	 * 仅 {@code DRAFT} 可改，非草稿抛 40902；每次改动版本号自增（乐观锁 {@code @Version}）。
	 *
	 * @param menuId 菜单ID
	 * @param cmd    更新请求
	 * @return 更新后的新版本号
	 */
	int update(Long menuId, MenuUpdateCmd cmd);

	/**
	 * 发布菜单（M1-10）
	 * <p>
	 * 执行顺序：校验菜单存在且菜品有效 → 状态置 {@code PUBLISHED} + {@code locked=true}
	 * + {@code publishedAt} → 调 {@code MenuSnapshotService.freeze(menuId)} 写入不可变快照
	 * → 发布 {@code MenuPublishedEvent}（由订阅方 {@code @TransactionalEventListener(AFTER_COMMIT)} 处理）。
	 *
	 * @param menuId 菜单ID
	 * @return 发布结果（菜单ID、状态、发布时间、快照条数、版本号）
	 */
	PublishResultVO publish(Long menuId);

	/**
	 * 下架菜单（M1-11）
	 * <p>
	 * {@code PUBLISHED → OFFLINE}，记录下架时间与原因；已冻结的快照与历史订单不受影响。
	 *
	 * @param menuId 菜单ID
	 * @param reason 下架原因，可为空
	 */
	void unpublish(Long menuId, String reason);

	/**
	 * 查询指定日期的有效菜单（M1-12，**M2 下单取价唯一入口**）
	 * <p>
	 * 取“生效日期不晚于 {@code date}、状态为 {@code PUBLISHED}、且已冻结快照”的菜单中
	 * 生效日最晚的一份，**必须带快照明细**；{@code items[].menuPrice} 为下单取价字段
	 * （菜单级价格优先，为空回退 {@code unitPrice}）。无可用菜单返回 {@code null}。
	 *
	 * @param date 就餐日期，为空取当天
	 * @return 当日菜单（含快照明细），无则返回 {@code null}
	 */
	MenuVO getCurrent(LocalDate date);

	/**
	 * 分页查询历史菜单与版本（M1-13）
	 * <p>
	 * {@code withItems=true} 时带明细：已发布/已下架菜单取冻结快照、草稿菜单取当前菜品，
	 * 供“历史菜单一键复用”生成新草稿。
	 *
	 * @param q 查询条件（日期区间、状态、菜单ID、是否带明细 + 分页）
	 * @return 分页菜单
	 */
	PageResult<MenuVO> page(MenuQuery q);
}
