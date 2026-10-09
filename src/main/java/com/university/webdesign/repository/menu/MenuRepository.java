package com.university.webdesign.repository.menu;

import com.university.webdesign.domain.menu.Menu;
import com.university.webdesign.domain.menu.MenuStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

/**
 * 菜单头信息数据访问接口，对应表 {@code menu}。
 * <p>
 * {@code MenuService.getCurrent}（M2 下单取价的唯一入口）依赖
 * {@link #findByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDescMenuIdDesc}
 * 取“生效日期不晚于指定日期、生效日最晚”的已发布菜单。
 */
public interface MenuRepository extends JpaRepository<Menu, Long>, JpaSpecificationExecutor<Menu>
{
	/**
	 * 查询指定状态的菜单，按生效日期倒序、菜单ID倒序
	 *
	 * @param status 菜单状态
	 * @return 菜单列表
	 */
	List<Menu> findByStatusOrderByEffectiveDateDescMenuIdDesc(MenuStatus status);

	/**
	 * 查询生效日期不晚于 {@code effectiveDate} 的指定状态菜单，按生效日期倒序、菜单ID倒序
	 *
	 * @param status        菜单状态，取 {@code PUBLISHED} 即“当日可点餐菜单”
	 * @param effectiveDate 截止生效日期（含）
	 * @return 菜单列表，最晚生效的一份在最前
	 */
	List<Menu> findByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDescMenuIdDesc(
			MenuStatus status, LocalDate effectiveDate);

	/**
	 * 统计指定状态的菜单数
	 *
	 * @param status 菜单状态
	 * @return 菜单数
	 */
	long countByStatus(MenuStatus status);
}
