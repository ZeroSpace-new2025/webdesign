package com.university.webdesign.api.operation;

import com.university.webdesign.common.DateRange;
import com.university.webdesign.common.PageQuery;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.PermCodes;
import com.university.webdesign.common.Result;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.config.RequiresPerm;
import com.university.webdesign.service.operation.BlanketOrderService;
import com.university.webdesign.service.operation.dto.CategoryStatVO;
import com.university.webdesign.service.operation.dto.CategorySumVO;
import com.university.webdesign.service.operation.dto.DailyStatVO;
import com.university.webdesign.service.operation.dto.PrintFormat;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 总括订单与每日统计 REST 控制器。
 * <p>
 * 对应《对外方法表》M3 的 `ApiBlanketOrderController`（基础路径 `/api/v1/operation`）：
 * 手动聚合（M3-01）、总括订单查询（M3-02）、生产单打印（M3-03）、分类汇总（M3-04）、
 * 每日汇总快照（M3-05）、刷新快照（M3-06）。
 * <p>
 * 本层只做协议转换与参数校验，业务规则（截止时间校验、聚合算法）全部在
 * {@link BlanketOrderService} 中；文件类接口直接返回文件流，不包 {@code Result}。
 */
@RestController
@RequestMapping("/api/v1/operation")
public class ApiBlanketOrderController
{
	private final BlanketOrderService blanketOrderService;

	public ApiBlanketOrderController(BlanketOrderService blanketOrderService) {
		this.blanketOrderService = blanketOrderService;
	}

	/**
	 * M3-01 手动触发聚合（兜底补偿入口）
	 * <p>
	 * 入参走查询串（{@code ?date=&force=}），不接收请求体：正带 {@code @RequestBody} 又允许
	 * 空体时，Spring 仍会对无 {@code Content-Type: application/json} 的请求抛
	 * {@code HttpMediaTypeNotSupportedException}，用表单或裸 POST 调用会得到 500。
	 *
	 * @param date  汇总日期，为空取当天
	 * @param force 是否强制覆盖既有快照，默认 false
	 * @return 当日汇总快照
	 */
	@PostMapping("/blanket-orders/aggregate")
	@RequiresPerm(value = PermCodes.OPERATION_AGGREGATE,
			roles = {RoleCodes.KITCHEN_SUPERVISOR, RoleCodes.MANAGER})
	public Result<DailyStatVO> aggregate(
			@RequestParam(value = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "force", required = false, defaultValue = "false") boolean force) {
		return Result.success(blanketOrderService.aggregate(date, force));
	}

	/**
	 * M3-02 查询总括订单（按菜品分类汇总当日需求）
	 *
	 * @param date       汇总日期，为空取当天
	 * @param categoryId 分类ID，可为空
	 * @param page       分页参数（`page`/`size`）
	 * @return 分页的“分类 → 菜品 → 总量/单位”列表
	 */
	@GetMapping("/blanket-orders")
	@RequiresPerm(value = PermCodes.OPERATION_AGGREGATE,
			roles = {RoleCodes.KITCHEN_SUPERVISOR, RoleCodes.DELIVERY_STAFF, RoleCodes.MANAGER})
	public Result<PageResult<CategoryStatVO>> list(
			@RequestParam(value = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "categoryId", required = false) Long categoryId,
			@ModelAttribute OperationPageRequest page) {
		List<CategoryStatVO> all = blanketOrderService.listAggregated(date, categoryId);
		return Result.success(PageResult.ofPage(all, page.normalizedPageNum(), page.normalizedPageSize()));
	}

	/**
	 * M3-03 打印生产单
	 *
	 * @param request 打印入参（日期、分类、格式）
	 * @return 生产单文件流
	 */
	@PostMapping("/blanket-orders/print")
	@RequiresPerm(value = PermCodes.OPERATION_AGGREGATE,
			roles = {RoleCodes.KITCHEN_SUPERVISOR, RoleCodes.MANAGER})
	public ResponseEntity<byte[]> print(@Valid @RequestBody PrintProductionRequest request) {
		Resource resource = blanketOrderService.printProductionOrder(
				request.getDate(), request.getCategoryIds(), request.getFormat());
		return download(resource, request.getFormat(), "production-order.txt", "production-order.xls");
	}

	/**
	 * M3-03 打印生产单（GET 形态，便于浏览器直接下载）
	 *
	 * @param date        汇总日期，为空取当天
	 * @param categoryIds 分类ID列表，可为空
	 * @param format      打印格式：TXT / XLSX / PRINT / PDF
	 * @return 生产单文件流
	 */
	@GetMapping("/blanket-orders/print")
	@RequiresPerm(value = PermCodes.OPERATION_AGGREGATE,
			roles = {RoleCodes.KITCHEN_SUPERVISOR, RoleCodes.MANAGER})
	public ResponseEntity<byte[]> printByQuery(
			@RequestParam(value = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
			@RequestParam(value = "format", required = false, defaultValue = "TXT") String format) {
		PrintFormat printFormat = PrintFormat.parse(format);
		Resource resource = blanketOrderService.printProductionOrder(date, categoryIds, printFormat);
		return download(resource, printFormat, "production-order.txt", "production-order.xls");
	}

	/**
	 * M3-04 分类维度汇总
	 *
	 * @param date 汇总日期，为空取当天
	 * @return 分类粒度的总量与金额
	 */
	@GetMapping("/blanket-orders/by-category")
	@RequiresPerm(value = PermCodes.OPERATION_AGGREGATE,
			roles = {RoleCodes.KITCHEN_SUPERVISOR, RoleCodes.DELIVERY_STAFF, RoleCodes.MANAGER})
	public Result<List<CategorySumVO>> byCategory(
			@RequestParam(value = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return Result.success(blanketOrderService.sumByCategory(date));
	}

	/**
	 * M3-05 查询每日汇总快照
	 *
	 * @param dateFrom 起始日期（含），可为空
	 * @param dateTo   结束日期（含），可为空
	 * @param page     分页参数（`page`/`size`）
	 * @return 分页的每日汇总
	 */
	@GetMapping("/statistics/daily")
	@RequiresPerm(value = PermCodes.OPERATION_AGGREGATE,
			roles = {RoleCodes.KITCHEN_SUPERVISOR, RoleCodes.MANAGER})
	public Result<PageResult<DailyStatVO>> dailyStat(
			@RequestParam(value = "dateFrom", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
			@RequestParam(value = "dateTo", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
			@ModelAttribute OperationPageRequest page) {
		DateRange range = new DateRange(dateFrom, dateTo);
		PageQuery query = new PageQuery();
		query.setPageNum(page.normalizedPageNum());
		query.setPageSize(page.normalizedPageSize());
		return Result.success(blanketOrderService.pageDailyStat(range, query));
	}

	/**
	 * M3-06 刷新汇总快照（订单变更后重算，幂等）
	 *
	 * @param date 汇总日期，为空取当天
	 * @return 重算后的当日汇总快照
	 */
	@PostMapping("/statistics/daily/refresh")
	@RequiresPerm(value = PermCodes.OPERATION_AGGREGATE,
			roles = {RoleCodes.KITCHEN_SUPERVISOR, RoleCodes.MANAGER})
	public Result<DailyStatVO> refresh(
			@RequestParam(value = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		blanketOrderService.refreshDailyStat(date);
		return Result.success(blanketOrderService.aggregate(date, false));
	}

	// ------------------------------------------------------------------ 内部方法

	/**
	 * 把生产单资源包装成文件下载响应（成功时不包 {@code Result}）
	 *
	 * @param resource   文件资源
	 * @param format     打印格式（决定响应类型与后缀）
	 * @param textName   文本类文件名
	 * @param excelName  表格类文件名
	 * @return 文件下载响应
	 */
	private ResponseEntity<byte[]> download(Resource resource, PrintFormat format, String textName, String excelName) {
		boolean excel = format == PrintFormat.XLSX;
		return OperationFileResponses.download(resource, excel, excel ? excelName : textName);
	}
}
