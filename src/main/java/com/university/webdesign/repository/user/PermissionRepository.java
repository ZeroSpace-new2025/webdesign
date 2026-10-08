package com.university.webdesign.repository.user;

import com.university.webdesign.domain.user.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 权限点（`permissions`）数据访问接口。
 * <p>
 * 归属用户与报表中心（M4），只允许 `service/impl/user` 下的实现类使用。
 */
public interface PermissionRepository extends JpaRepository<Permission, Long>
{
	/**
	 * 按权限点编码查询
	 *
	 * @param permCode 权限点编码
	 * @return 权限点
	 */
	Optional<Permission> findByPermCode(String permCode);

	/**
	 * 按权限点编码集合查询
	 *
	 * @param permCodes 权限点编码集合
	 * @return 权限点列表
	 */
	List<Permission> findAllByPermCodeIn(Collection<String> permCodes);

	/**
	 * 全量权限点，按模块、编码排序
	 *
	 * @return 权限点列表
	 */
	List<Permission> findAllByOrderByModuleAscPermCodeAsc();

	/**
	 * 按模块查询权限点
	 *
	 * @param module 模块名
	 * @return 权限点列表
	 */
	List<Permission> findAllByModuleOrderByPermCodeAsc(String module);
}
