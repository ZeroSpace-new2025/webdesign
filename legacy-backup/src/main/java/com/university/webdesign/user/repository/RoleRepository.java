package com.university.webdesign.user.repository;

import com.university.webdesign.user.data.RoleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long>
{
	@EntityGraph(attributePaths = "permissions")
	List<RoleEntity> findAllByOrderByIdAsc();

	@EntityGraph(attributePaths = "permissions")
	Optional<RoleEntity> findWithPermissionsById(Long id);

	Optional<RoleEntity> findByCodeIgnoreCase(String code);
}
