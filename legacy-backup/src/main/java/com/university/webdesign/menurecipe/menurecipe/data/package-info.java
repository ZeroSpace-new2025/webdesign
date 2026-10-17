/**
 * 菜品与菜单中心 —— 持久化实体层。
 * <p>
 * 放置 JPA 实体（{@code @Entity}）与领域枚举，按 {@code demand.md} 的数据库设计包括：
 * Recipe（标准菜品）、Menu（菜单头：名称、状态、生效时间）、Menu_Item（菜单与菜品关联及当前价格）。
 * 实体约定见 AGENTS.md 第 3 节：显式 {@code @Table}/{@code @Column}（下划线命名）、
 * Lombok 的 {@code @Data}/{@code @NoArgsConstructor}/{@code @AllArgsConstructor}，
 * 参照 {@code com.university.webdesign.ordertransaction.data.Order}。
 * <p>
 * 数据保护机制（修改或删除菜品只影响未来菜单、不影响历史订单）应在服务层配合快照实现。
 */
package com.university.webdesign.menurecipe.data;
