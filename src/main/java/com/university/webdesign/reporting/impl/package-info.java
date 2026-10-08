/**
 * 用户与报表中心 —— 报表模块（服务实现层）。
 * <p>
 * 放置 {@code com.university.webdesign.reporting.service} 各接口的实现类，
 * 命名“接口名 + Impl”，用 {@code @Component} 注册；构造器注入依赖。
 * <p>
 * 实现要点：月度统计结果写入 Monthly_Report 缓存表以加速报表展示（见 {@code demand.md}）。
 */
package com.university.webdesign.reporting.impl;
