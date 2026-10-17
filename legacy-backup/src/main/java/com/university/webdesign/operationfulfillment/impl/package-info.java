/**
 * 运营与履约系统 —— 服务实现层。
 * <p>
 * 放置 {@code com.university.webdesign.operationfulfillment.service} 各接口的实现类，
 * 命名“接口名 + Impl”，用 {@code @Component} 注册；构造器注入依赖。
 * <p>
 * 实现要点（见 AGENTS.md 第 4 节业务不变量）：
 * <ul>
 *     <li>总括订单只在订餐截止时间之后聚合当日有效订单；</li>
 *     <li>生产单按菜品分类汇总总数量；</li>
 *     <li>配送单打印权限在“配餐开始时间”（默认 11:30）后才开放，按员工/工位批量生成。</li>
 * </ul>
 */
package com.university.webdesign.operationfulfillment.impl;
