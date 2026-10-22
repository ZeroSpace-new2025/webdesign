/**
 * 运营与履约系统 —— 数据访问层。
 * <p>
 * 放置 Spring Data JPA 仓储接口，按 {@code demand.md} 的数据库设计主要为：
 * Daily_Statistics（每日汇总快照，用于快速查询当日总需求）、Delivery_Task（配送任务记录）。
 * 参照 {@code com.university.webdesign.ordertransaction.repository.OrderRepository}。
 */
package com.university.webdesign.operationfulfillment.repository;
