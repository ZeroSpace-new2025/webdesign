package com.university.webdesign.user.repository;

import com.university.webdesign.user.data.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long>
{
	Optional<PermissionEntity> findByCodeIgnoreCase(String code);

	List<PermissionEntity> findAllByOrderByIdAsc();
}
