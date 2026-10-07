package com.university.webdesign.operationfulfillment.service;

import com.university.webdesign.operationfulfillment.dto.ProductionSummaryDTO;
import com.university.webdesign.operationfulfillment.dto.DeliveryTaskDTO;
import java.util.List;

/**
 * 运营与履约系统 Service 接口
 * 核心职责：处理“怎么做”(后厨备料) 和 “怎么送”(配送管理)
 */
public interface FulfillmentService {

    // ================== 总括订单 (Blanket Order) ==================

    /**
     * 聚合算法：在“订餐截止时间”后，自动汇总当日所有有效订单。
     * 该方法通常由定时任务(Scheduled)在截止时间后触发。
     * 将汇总数据快照存入 Daily_Statistics 表。
     *
     * @return 是否聚合成功
     */
    boolean aggregateDailyOrders();

    /**
     * 获取生产单汇总数据 (供厨房主管查看/打印)
     * 从 Daily_Statistics 表中快速查询当日总需求。
     *
     * @return 按菜品分类统计的总数量列表
     */
    List<ProductionSummaryDTO> getProductionSummary();

    // ================== 配送管理 (Delivery) ==================

    /**
     * 时间触发校验：检查当前是否到达“配餐开始时间”(默认11:30)。
     * 只有到达时间后，才开放打印和生成权限。
     *
     * @return true表示已到达可以配餐的时间，false表示时间未到
     */
    boolean isDeliveryTimeReached();

    /**
     * 批量打印/生成配送单：
     * 按员工/工位维度批量生成配送单。
     * 包含详细信息（菜名、分量、工位、电话）。
     *
     * @return 配送任务列表 (Delivery_Task 表数据)
     */
    List<DeliveryTaskDTO> generateDeliveryTasks();

    /**
     * 获取已生成的配送单列表 (用于页面展示或打印)
     * @return 配送任务列表
     */
    List<DeliveryTaskDTO> getDeliveryTasks();
}