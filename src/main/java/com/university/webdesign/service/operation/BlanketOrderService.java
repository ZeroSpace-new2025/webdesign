package com.university.webdesign.service.operation;

import com.university.webdesign.common.DateRange;
import com.university.webdesign.common.PageQuery;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.service.operation.dto.CategoryStatVO;
import com.university.webdesign.service.operation.dto.CategorySumVO;
import com.university.webdesign.service.operation.dto.DailyStatVO;
import com.university.webdesign.service.operation.dto.PrintFormat;
import org.springframework.core.io.Resource;

import java.time.LocalDate;
import java.util.List;

/**
 * 运营与履约系统——总括订单服务。
 * <p>
 * 对应《对外方法表》4.2 的 `BlanketOrderService`。核心职责：在“订餐截止时间”之后
 * 汇总当日所有有效订单，按菜品分类统计总数量，写入 `daily_statistics` 快照供厨房备料与生产单打印。
 * <p>
 * 取数一律走 {@code OrderQueryService.listValidByDate(date)}，**不得直接访问订单表**。
 */
public interface BlanketOrderService
{
	/**
	 * 聚合当日总括订单（M3-01）
	 * <p>
	 * 算法：`listValidByDate` 取当日有效订单 → 按 `recipeId` 汇总数量与金额 → 写 `daily_statistics`。
	 * `force=false` 且当日已有快照时直接返回既有结果（幂等）。
	 *
	 * @param date  汇总日期，为空取当天
	 * @param force 是否强制覆盖既有快照
	 * @return 当日汇总快照
	 */
	DailyStatVO aggregate(LocalDate date, boolean force);

	/**
	 * 查询总括订单（M3-02）：按菜品分类汇总当日需求，供厨房备料
	 *
	 * @param date       汇总日期，为空取当天
	 * @param categoryId 分类ID（当前实现按分类名匹配，为空表示全部分类）
	 * @return “分类 → 菜品 → 总量/单位”列表
	 */
	List<CategoryStatVO> listAggregated(LocalDate date, Long categoryId);

	/**
	 * 打印生产单（M3-03）
	 * <p>
	 * 校验 `date` 已过订餐截止时间，未过则抛 42201。
	 *
	 * @param date       汇总日期
	 * @param categoryIds 指定分类（当前实现按分类名匹配），为空表示全部分类
	 * @param fmt        打印格式
	 * @return 生产单文件资源
	 */
	Resource printProductionOrder(LocalDate date, List<Long> categoryIds, PrintFormat fmt);

	/**
	 * 分类维度汇总（M3-04）
	 *
	 * @param date 汇总日期，为空取当天
	 * @return 分类粒度的总量与金额
	 */
	List<CategorySumVO> sumByCategory(LocalDate date);

	/**
	 * 查询每日汇总快照（M3-05）
	 *
	 * @param range 日期区间
	 * @param page  分页参数
	 * @return 分页的每日汇总
	 */
	PageResult<DailyStatVO> pageDailyStat(DateRange range, PageQuery page);

	/**
	 * 刷新汇总快照（M3-06）：订单变更后重算某日快照（幂等，覆盖写）
	 *
	 * @param date 汇总日期，为空取当天
	 */
	void refreshDailyStat(LocalDate date);
}
