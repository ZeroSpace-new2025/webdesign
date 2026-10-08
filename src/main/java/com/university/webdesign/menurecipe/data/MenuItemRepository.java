package com.university.webdesign.menurecipe.data;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 菜单项数据访问层
 */
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
}
