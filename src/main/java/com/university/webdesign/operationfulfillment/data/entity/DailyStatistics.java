package com.university.webdesign.operationfulfillment.data.entity;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DailyStatistics {
    private Long id;
    private LocalDate statDate;       // 统计日期
    private String dishName;          // 菜品名称
    private Integer totalQuantity;    // 总数量
    private String unit;              // 单位(两/份)
}