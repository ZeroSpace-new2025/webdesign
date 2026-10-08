package com.university.webdesign.menurecipe.service;

import com.university.webdesign.menurecipe.api.MenuDTO;
import com.university.webdesign.menurecipe.api.MenuItemData;
import com.university.webdesign.menurecipe.api.MenuQueryData;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

/**
 * 菜单服务（跨模块能力契约）
 * <p>
 * 本接口是订单与交易核心对“菜品与菜单中心”的全部依赖入口，
 * 只有这里出现的方法才允许被其他模块调用。
 * <p>
 * 实现类放在同级的 {@code impl} 包下，命名 {@code MenuServiceImpl}
 * （参照 {@code ordertransaction.service.OrderService} 与 {@code ordertransaction.impl.OrderServiceImpl}）。
 * <p>
 * //todo 确认：本接口由订单模块按调用需要先行约定（订单模块下单时必须知道“今日可点什么菜、什么价”），
 * 请菜品与菜单中心（方家乐）确认方法签名与语义后补齐实现，尤其是：
 * <ul>
 *     <li>{@link #getActiveMenu(LocalDate)} 的返回语义（按日期生效的已发布菜单，无则返回 null）；</li>
 *     <li>{@code MenuDTO.menuItems} 的元素类型（约定为 {@link MenuItemData}），以便调用方获得编译期类型安全。</li>
 * </ul>
 */
@Component
public interface MenuService
{
	/**
	 * 获取指定日期生效的已发布菜单
	 *
	 * @param date 就餐日期
	 * @return 已发布菜单；该日期无生效菜单时返回 null
	 */
	MenuDTO getActiveMenu(LocalDate date);

	/**
	 * 获取当前菜单
	 *
	 * @return 当前菜单
	 * 根据ID获取菜单
	 */
	MenuDTO getMenu();
	
	MenuDTO getById(Long id);

	/**
	 * 按条件查询菜单
	 *
	 * @param queryData 查询条件
	 * @return 菜单列表
	 */
	List<MenuDTO> query(MenuQueryData queryData);

	/**
	 * 获取全部菜单
	 *
	 * @return 菜单列表
	 */
	List<MenuDTO> getAll();

	/**
	 * 新建菜单（可携带菜单项）
	 */
	MenuDTO create(MenuDTO menuDTO);

	/**
	 * 修改菜单基本信息或其菜单项
	 */
	MenuDTO update(MenuDTO menuDTO);

	/**
	 * 删除菜单
	 */
	void delete(Long id);

	/**
	 * 发布菜单，发布后可供点餐
	 */
	MenuDTO publish(Long id);

	/**
	 * 下架菜单
	 */
	MenuDTO offline(Long id);

	/**
	 * 复制一份历史菜单，便于复用（生成草稿副本）
	 */
	MenuDTO copyMenu(Long sourceId);

	/**
	 * 在菜单层级调整指定菜品的售价
	 */
	MenuDTO adjustItemPrice(Long menuId, Long itemId, BigDecimal price);
	List<MenuDTO> getAllMenus();
}
