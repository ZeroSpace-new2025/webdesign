/**
 * 用户与报表中心 —— 持久化实体层。
 * <p>
 * 放置 JPA 实体与领域枚举，按 {@code demand.md} 的数据库设计包括：
 * User（用户基础信息及部门、工位信息）、Role_Permission（角色与权限映射）、
 * Monthly_Report（月度统计结果缓存）。
 * 实体约定见 AGENTS.md 第 3 节，参照 {@code com.university.webdesign.ordertransaction.data.Order}。
 * <p>
 * 角色枚举建议在此定义，并与订单模块使用的 {@code MANAGER} / {@code FINANCE} 编码保持一致。
 */
package com.university.webdesign.user.data;
