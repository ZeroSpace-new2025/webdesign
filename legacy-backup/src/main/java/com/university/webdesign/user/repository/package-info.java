/**
 * 用户与报表中心 —— 数据访问层。
 * <p>
 * 放置 Spring Data JPA 仓储接口，按 {@code demand.md} 的数据库设计主要为：
 * User（员工信息、部门、工位、电话）、Role_Permission（角色与权限映射）、
 * Monthly_Report（月度统计结果缓存表）。
 * 参照 {@code com.university.webdesign.ordertransaction.repository.OrderRepository}。
 */
package com.university.webdesign.user.repository;
