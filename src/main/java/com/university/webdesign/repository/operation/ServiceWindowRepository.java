package com.university.webdesign.repository.operation;

import com.university.webdesign.domain.operation.ServiceWindow;
import com.university.webdesign.domain.operation.WindowScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 服务时间窗口配置仓储（`service_window`）。
 * <p>
 * 查询全部围绕“某作用域下 `effective_from` 已生效（为空视为立即生效）的最新一条”展开：
 * `effective_from` 倒序（NULL 在前，即立即生效的配置优先），同日按主键倒序（后配的优先），
 * 由 {@code ServiceWindowServiceImpl.get} 组合出 `DEPT` 优先于 `GLOBAL` 的取值优先级。
 */
@Repository
public interface ServiceWindowRepository extends JpaRepository<ServiceWindow, Long>
{
	/**
	 * 查询某作用域下已生效的配置，按生效起始日期倒序、主键倒序
	 *
	 * @param scope 作用域
	 * @param date  目标日期
	 * @return 已生效配置列表（生效日期最新在前，立即生效的排在最前）
	 */
	@Query("select w from ServiceWindow w where w.scope = :scope "
			+ "and (w.effectiveFrom is null or w.effectiveFrom <= :date) "
			+ "order by w.effectiveFrom desc nulls first, w.id desc")
	List<ServiceWindow> findEffective(@Param("scope") WindowScope scope, @Param("date") LocalDate date);

	/**
	 * 查询某作用域 + 某部门下已生效的配置，按生效起始日期倒序、主键倒序
	 *
	 * @param scope  作用域
	 * @param deptId 部门ID
	 * @param date   目标日期
	 * @return 已生效配置列表
	 */
	@Query("select w from ServiceWindow w where w.scope = :scope and w.deptId = :deptId "
			+ "and (w.effectiveFrom is null or w.effectiveFrom <= :date) "
			+ "order by w.effectiveFrom desc nulls first, w.id desc")
	List<ServiceWindow> findEffectiveByDept(@Param("scope") WindowScope scope,
			@Param("deptId") Long deptId, @Param("date") LocalDate date);

	/**
	 * 判断同一作用域（同部门）在指定生效日期是否已存在配置，用于新增时的重叠校验
	 *
	 * @param scope         作用域
	 * @param deptId        部门ID，可为空（全局配置传 null）
	 * @param effectiveFrom 生效起始日期，可为空
	 * @return 已存在时返回 true
	 */
	boolean existsByScopeAndDeptIdAndEffectiveFrom(WindowScope scope, Long deptId, LocalDate effectiveFrom);
}
