package com.university.webdesign.reporting.repository;

import com.university.webdesign.reporting.data.ReportOrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportOrderRepository extends JpaRepository<ReportOrderEntity, Long>
{
	@EntityGraph(attributePaths = "items")
	@Query("""
		select distinct reportOrder
		from ReportOrderEntity reportOrder
		where reportOrder.createdAt >= :start
		and reportOrder.createdAt < :end
		order by reportOrder.createdAt asc
		""")
	List<ReportOrderEntity> findForPeriod(
		@Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end);

	@EntityGraph(attributePaths = "items")
	@Query("""
		select distinct reportOrder
		from ReportOrderEntity reportOrder
		where reportOrder.userId = :userId
		and reportOrder.createdAt >= :start
		and reportOrder.createdAt < :end
		order by reportOrder.createdAt asc
		""")
	List<ReportOrderEntity> findForUserAndPeriod(
		@Param("userId") Long userId,
		@Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end);

	@EntityGraph(attributePaths = "items")
	List<ReportOrderEntity> findAllByOrderByCreatedAtAsc();
}
