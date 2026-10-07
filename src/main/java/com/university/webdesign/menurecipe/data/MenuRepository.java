package com.university.webdesign.menurecipe.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 菜单数据访问层
 */
public interface MenuRepository extends JpaRepository<Menu, Long>, JpaSpecificationExecutor<Menu> {

	/**
	 * 查询所有已发布的菜单
	 */
	List<Menu> findByStatus(String status);
}
