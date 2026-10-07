package com.university.webdesign.operationfulfillment.data.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DeliveryTask {
    private Long id;
    private String employeeId;        // 员工ID
    private String station;           // 工位
    private String phone;             // 电话
    private String dishDetails;       // 菜品明细 (JSON格式存储，或者拆分成子表)
    private Integer status;           // 状态 (0:待配送, 1:已送达)
    private LocalDateTime createTime; // 创建时间
}