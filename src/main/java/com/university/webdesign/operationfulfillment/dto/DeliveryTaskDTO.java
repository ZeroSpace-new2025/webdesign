package com.university.webdesign.operationfulfillment.dto;

import lombok.Data;
import java.util.List;

@Data
public class DeliveryTaskDTO {
    /**
     * 员工/工位维度标识 (如：工位号或员工ID)
     */
    private String stationOrEmployeeId;

    /**
     * 员工姓名 (可选，方便显示)
     */
    private String employeeName;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 该员工/工位包含的菜品明细
     */
    private List<OrderItemDetail> items;

    @Data
    public static class OrderItemDetail {
        private String dishName;  // 菜名
        private Integer quantity; // 分量
    }
}