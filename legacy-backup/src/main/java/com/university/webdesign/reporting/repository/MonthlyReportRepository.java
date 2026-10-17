package com.university.webdesign.reporting.repository;

import com.university.webdesign.reporting.data.MonthlyReportEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthlyReportRepository extends JpaRepository<MonthlyReportEntity, Long>
{
	@EntityGraph(attributePaths = "items")
	Optional<MonthlyReportEntity> findByReportMonth(String reportMonth);
}
