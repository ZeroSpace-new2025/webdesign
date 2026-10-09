package com.university.webdesign.repository.user;

import com.university.webdesign.domain.user.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 角色（`role`）数据访问接口。
 * <p>
 * 归属用户与报表中心（M4），只允许 `impl/user` 下的实现类与数据初始化器使用。
 * <p>
 * 角色权限是 {@code permission_list} 位图（{@code PermissionList}），属于基本属性而非关联集合，
 * 随实体一起加载，因此这里不再需要 {@code @EntityGraph} 抓取权限。
 */
public interface RoleRepository extends JpaRepository<Role, Long>
{
	/**
	 * 按角色名称查询（忽略大小写）
	 *
	 * @param name 角色业务名称（如 {@code MANAGER}）
	 * @return 角色
	 */
	Optional<Role> findByNameIgnoreCase(String name);

	/**
	 * 判断角色名称是否已存在
	 *
	 * @param name 角色业务名称
	 * @return 已存在返回 true
	 */
	boolean existsByNameIgnoreCase(String name);

	/**
	 * 按ID集合查询
	 *
	 * @param ids 角色ID集合
	 * @return 角色列表
	 */
	List<Role> findAllByIdIn(Collection<Long> ids);

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
	 * @param keyword  关键字，模糊匹配角色名称，可为空
	 * @param pageable 分页参数
	 * @return 分页结果
	 */
	@Query("""
			select role from Role role
			where (:keyword is null
				or lower(role.name) like lower(concat('%', :keyword, '%')))
			order by role.id asc
			""")
	Page<Role> search(@Param("keyword") String keyword, Pageable pageable);
}
