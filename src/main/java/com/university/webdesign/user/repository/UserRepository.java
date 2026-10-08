package com.university.webdesign.user.repository;

import com.university.webdesign.user.data.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long>
{
	boolean existsByUsernameIgnoreCase(String username);

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	Optional<UserEntity> findWithRolesById(Long id);

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	Optional<UserEntity> findWithRolesByUsernameIgnoreCase(String username);

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	@Query("""
		select distinct user
		from UserEntity user
		left join user.roles role
		where (:keyword is null
			or lower(user.username) like lower(concat('%', :keyword, '%'))
			or lower(user.name) like lower(concat('%', :keyword, '%'))
			or lower(user.workstation) like lower(concat('%', :keyword, '%'))
			or lower(user.phone) like lower(concat('%', :keyword, '%')))
		and (:department is null or user.department = :department)
		and (:roleId is null or role.id = :roleId)
		and (:enabled is null or user.enabled = :enabled)
		order by user.id asc
		""")
	List<UserEntity> search(
		@Param("keyword") String keyword,
		@Param("department") String department,
		@Param("roleId") Long roleId,
		@Param("enabled") Boolean enabled);
}
