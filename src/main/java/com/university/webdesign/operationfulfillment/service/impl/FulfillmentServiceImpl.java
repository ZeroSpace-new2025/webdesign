package com.university.webdesign.operationfulfillment.service.impl;

import com.university.webdesign.operationfulfillment.service.FulfillmentService;
import com.university.webdesign.operationfulfillment.dto.ProductionSummaryDTO;
import com.university.webdesign.operationfulfillment.dto.DeliveryTaskDTO;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.LocalTime;
import java.util.List;

@Service // 注意：@Service加在实现类上，不要加在接口上
public class FulfillmentServiceImpl implements FulfillmentService {

    // 假设你注入了 data 层的 Repository
    // @Autowired private DailyStatisticsRepository dailyRepo;
    // @Autowired private DeliveryTaskRepository deliveryRepo;

    @Override
    // 需求：在“订餐截止时间”后，自动汇总。使用 Spring 定时任务 (比如每天10:00触发)
    @Scheduled(cron = "0 0 10 * * ?")
    public boolean aggregateDailyOrders() {
        // TODO: 1. 查询当日所有有效订单
        // TODO: 2. 按菜品分组求和
        // TODO: 3. 保存到 Daily_Statistics 表
        System.out.println("执行每日订单聚合任务...");
        return true;
    }

    @Override
    public List<ProductionSummaryDTO> getProductionSummary() {
        // TODO: 从 Daily_Statistics 表查询数据并转换为 DTO 返回
        return null;
    }

    @Override
    public boolean isDeliveryTimeReached() {
        // 需求：默认11:30后开放打印权限
        LocalTime now = LocalTime.now();
        LocalTime startTime = LocalTime.of(11, 30);
        return now.isAfter(startTime); // 当前时间晚于11:30返回true
    }

    @Override
    public List<DeliveryTaskDTO> generateDeliveryTasks() {
        // 需求：按员工/工位维度批量生成配送单
        if (!isDeliveryTimeReached()) {
            throw new RuntimeException("未到配餐开始时间(11:30)，无法生成配送单");
        }
        // TODO: 1. 查询已聚合的订单数据
        // TODO: 2. 按员工/工位分组组装数据
        // TODO: 3. 保存到 Delivery_Task 表
        return null;
    }

    @Override
    public List<DeliveryTaskDTO> getDeliveryTasks() {
        // TODO: 从 Delivery_Task 表查询并返回
        return null;
    }
}