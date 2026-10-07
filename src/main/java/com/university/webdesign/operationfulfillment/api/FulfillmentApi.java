package com.university.webdesign.operationfulfillment.api;

import com.university.webdesign.operationfulfillment.dto.ProductionSummaryDTO;
import com.university.webdesign.operationfulfillment.dto.DeliveryTaskDTO;
import com.university.webdesign.operationfulfillment.service.FulfillmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fulfillment")
public class FulfillmentApi {

    @Autowired
    private FulfillmentService fulfillmentService;

    /**
     * 厨房主管：查看/获取生产单（按菜品统计）
     */
    @GetMapping("/production-summary")
    public List<ProductionSummaryDTO> getProductionSummary() {
        return fulfillmentService.getProductionSummary();
    }

    /**
     * 配餐员：检查是否到达配餐时间 (11:30)
     */
    @GetMapping("/check-delivery-time")
    public boolean checkDeliveryTime() {
        return fulfillmentService.isDeliveryTimeReached();
    }

    /**
     * 配餐员：批量生成配送单（点击打印时触发）
     */
    @PostMapping("/delivery-tasks/generate")
    public List<DeliveryTaskDTO> generateDeliveryTasks() {
        return fulfillmentService.generateDeliveryTasks();
    }

    /**
     * 配餐员：获取已生成的配送单列表
     */
    @GetMapping("/delivery-tasks")
    public List<DeliveryTaskDTO> getDeliveryTasks() {
        return fulfillmentService.getDeliveryTasks();
    }
}