package com.university.webdesign.repository.user;

import com.university.webdesign.domain.user.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 角色（`roles`）数据访问接口。
 * <p>
 * 归属用户与报表中心（M4），只允许 `service/impl/user` 下的实现类使用。
 */
public interface RoleRepository extends JpaRepository<Role, Long>
{
	/**
	 * 按角色编码查询（忽略大小写）
	 *
	 * @param roleCode 角色编码
	 * @return 角色
	 */
	Optional<Role> findByRoleCodeIgnoreCase(String roleCode);

	/**
	 * 按ID查询并抓取权限点
	 *
	 * @param id 角色ID
	 * @return 角色
	 */
	@EntityGraph(attributePaths = {"permissions"})
	Optional<Role> findWithPermissionsById(Long id);

	/**
	 * 按ID集合查询
	 *
	 * @param ids 角色ID集合
	 * @return 角色列表
	 */
	List<Role> findAllByIdIn(Collection<Long> ids);

	/**
	 * 判断角色编码是否已存在
	 *
	 * @param roleCode 角色编码
	 * @return 已存在返回 true
	 */
	boolean existsByRoleCodeIgnoreCase(String roleCode);

	/**
	 * 统计某角色的关联用户数，删除前校验用
	 *
	 * @param roleId 角色ID
	 * @return 关联用户数
	 */
	@Query("select count(user) from User user join user.roles role where role.id = :roleId")
	long countUsersByRoleId(@Param("roleId") Long roleId);

	/**
	 * 关键字分页查询角色
	 *
	 * @param keyword  关键字，模糊匹配角色编码/名称，可为空
	 * @param pageable 分页参数
	 * @return 分页结果
	 */
	@Query("""
			select role from Role role
			where (:keyword is null
				or lower(role.roleCode) like lower(concat('%', :keyword, '%'))
				or lower(role.roleName) like lower(concat('%', :keyword, '%')))
			order by role.id asc
			""")
	Page<Role> search(@Param("keyword") String keyword, Pageable pageable);
}
