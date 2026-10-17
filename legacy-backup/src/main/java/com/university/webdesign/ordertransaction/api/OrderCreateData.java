package com.university.webdesign.ordertransaction.api;

import lombok.Data;

import java.util.List;

/**
 * 下单请求体
 * <p>
 * //todo 确认：请求体字段目前由 {@code OrderServiceImpl} 在服务层校验（空列表、数量不匹配等都会返回失败结果）。
 * 若要与前端做参数级校验，需要先引入 {@code spring-boot-starter-validation} 依赖，
 * 该依赖是否加入 build.gradle 需全员确认后再统一添加。
 */
@Data
public class OrderCreateData
{
	/**
	 * 下单员工ID
	 * <p>
	 * //todo 确认：登录态由用户与报表中心负责，此处暂由前端传 operatorId；
	 * 接入 Spring Security 后应改为从认证上下文（SecurityContextHolder）中取当前登录用户，
	 * 不接受客户端自报身份。
	 */
	private Long operatorId;
	
	/**
	 * 所点菜品（菜谱）ID列表
	 */
	private List<Long> itemIds;
	
	/**
	 * 与 {@link #itemIds} 一一对应的数量列表
	 */
	private List<Integer> quantities;
}
