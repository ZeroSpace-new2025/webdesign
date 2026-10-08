package com.university.webdesign.repository.user;

import com.university.webdesign.domain.user.User;
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
 * 用户（`users`）数据访问接口。
 * <p>
 * 归属用户与报表中心（M4），只允许 `service/impl/user` 下的实现类使用；
 * 其他模块取员工信息必须走 `UserService.listByIds` / `getBrief` 契约，不得直接访问本表。
 */
public interface UserRepository extends JpaRepository<User, Long>
{
	/**
	 * 按工号查询（忽略大小写）
	 *
	 * @param employeeNo 工号
	 * @return 用户
	 */
	Optional<User> findByEmployeeNoIgnoreCase(String employeeNo);

	/**
	 * 按工号查询并抓取角色与权限
	 *
	 * @param employeeNo 工号
	 * @return 用户
	 */
	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	Optional<User> findWithRolesByEmployeeNoIgnoreCase(String employeeNo);

	/**
	 * 按ID查询并抓取角色与权限
	 *
	 * @param id 用户ID
	 * @return 用户
	 */
	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	Optional<User> findWithRolesById(Long id);

	/**
	 * 判断工号是否已存在
	 *
	 * @param employeeNo 工号
	 * @return 已存在返回 true
	 */
	boolean existsByEmployeeNoIgnoreCase(String employeeNo);

	/**
	 * 判断手机号是否已被占用（仅比较非空手机号）
	 *
	 * @param phone 手机号
	 * @return 已存在返回 true
	 */
	boolean existsByPhone(String phone);

	/**
	 * 批量按ID查询（供 M2 订单、M3 配送单补齐姓名/工位/电话）
	 *
	 * @param ids 用户ID集合
	 * @return 用户列表
	 */
	List<User> findAllByIdIn(Collection<Long> ids);

	/**
	 * 多条件分页查询用户
	 *
	 * @param keyword     关键字，模糊匹配工号/姓名/工位/电话，可为空
	 * @param deptId      部门ID，可为空
	 * @param workstation 工位，模糊匹配，可为空
	 * @param status      账号状态编码，可为空
	 * @param pageable    分页参数
	 * @return 分页结果
	 */
	@Query("""
			select user from User user
			where (:keyword is null
				or lower(user.employeeNo) like lower(concat('%', :keyword, '%'))
				or lower(user.name) like lower(concat('%', :keyword, '%'))
				or lower(user.workstation) like lower(concat('%', :keyword, '%'))
				or lower(user.phone) like lower(concat('%', :keyword, '%')))
			and (:deptId is null or user.deptId = :deptId)
			and (:workstation is null or lower(user.workstation) like lower(concat('%', :workstation, '%')))
			and (:status is null or user.status = :status)
			order by user.id asc
			""")
	Page<User> search(@Param("keyword") String keyword,
			@Param("deptId") Long deptId,
			@Param("workstation") String workstation,
			@Param("status") com.university.webdesign.domain.user.UserStatus status,
			Pageable pageable);
}
