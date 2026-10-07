package com.university.webdesign.operationfulfillment.dto;

import lombok.Data; // 假设你使用了Lombok，如果没有请自行生成Getter/Setter

@Data
public class ProductionSummaryDTO {
    /**
     * 菜品名称 (如：米饭、小炒肉)
     */
    private String dishName;

    /**
     * 总数量/总分量 (如：50两，30份)
     */
    private Integer totalQuantity;

    /**
     * 单位 (如：两、份)
     */
    private String unit;
}