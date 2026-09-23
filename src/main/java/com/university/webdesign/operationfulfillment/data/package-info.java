/**
 * 运营与履约系统 —— 持久化实体层。
 * <p>
 * 放置 JPA 实体与领域枚举，按 {@code demand.md} 的数据库设计包括：
 * Daily_Statistics（每日汇总快照）、Delivery_Task（配送任务）。
 * 实体约定见 AGENTS.md 第 3 节，参照 {@code com.university.webdesign.ordertransaction.data.Order}。
 * <p>
 * 注意：订单明细的快照字段（菜名、分类、单价、数量）已经由订单模块提供，
 * 生产单按分类汇总时直接使用 {@code OrderDTO.getItems()} 里的 {@code category} 即可，无需自建菜品表。
 */
package com.university.webdesign.operationfulfillment.data;
