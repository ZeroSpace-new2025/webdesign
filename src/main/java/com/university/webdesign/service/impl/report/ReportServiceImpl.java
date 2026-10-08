package com.university.webdesign.service.impl.report;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.PageQuery;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.PermCodes;
import com.university.webdesign.common.RoleCodes;
import com.university.webdesign.common.TabularExport;
import com.university.webdesign.common.UserContext;
import com.university.webdesign.common.UserContextHolder;
import com.university.webdesign.domain.report.MonthlyReport;
import com.university.webdesign.domain.report.MonthlyReportItem;
import com.university.webdesign.event.MonthlyReportGeneratedEvent;
import com.university.webdesign.repository.report.MonthlyReportRepository;
import com.university.webdesign.service.impl.report.support.MonthlyOrderReader;
import com.university.webdesign.service.order.OrderQueryService;
import com.university.webdesign.service.order.dto.ExportFormat;
import com.university.webdesign.service.order.dto.OrderDetailVO;
import com.university.webdesign.service.order.dto.OrderVO;
import com.university.webdesign.service.report.MonthConverter;
import com.university.webdesign.service.report.ReportService;
import com.university.webdesign.service.report.dto.DishSalesVO;
import com.university.webdesign.service.report.dto.MonthlyReportVO;
import com.university.webdesign.service.user.UserService;
import com.university.webdesign.service.user.dto.UserQuery;
import com.university.webdesign.service.user.dto.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 财务报表服务实现。
 * <p>
 * 对应《对外方法表》5.2 的 {@code ReportService} 全部方法。
 * <p>
 * 取数边界（硬性）：订单数据只经
 * {@link OrderQueryService}（由 {@link MonthlyOrderReader} 封装）读取，
 * **不新建订单实体、不直接访问订单表**。
 * <p>
 * 事务统一用普通 {@code @Transactional}（含查询），不加 {@code readOnly = true}。
 */
@Slf4j
@Service
public class ReportServiceImpl implements ReportService
{
	/**
	 * 生成时间展示格式
	 */
	private static final DateTimeFormatter DATE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final MonthlyReportRepository monthlyReportRepository;
	private final UserService userService;
	private final ApplicationEventPublisher eventPublisher;
	private final MonthlyOrderReader orderReader;

	/**
	 * 构造器注入
	 *
	 * @param monthlyReportRepository 月度报表仓库
	 * @param orderQueryService       订单取数契约（跨模块，只读）
	 * @param userService             员工信息契约（跨模块，只读；按部门筛明细时用 {@code page}）
	 * @param eventPublisher          领域事件发布器
	 */
	public ReportServiceImpl(
			MonthlyReportRepository monthlyReportRepository,
			OrderQueryService orderQueryService,
			UserService userService,
			ApplicationEventPublisher eventPublisher) {
		this.monthlyReportRepository = monthlyReportRepository;
		this.userService = userService;
		this.eventPublisher = eventPublisher;
		this.orderReader = new MonthlyOrderReader(orderQueryService);
	}

	@Override
	@Transactional
	public Long generateMonthly(YearMonth month, boolean force) {
		assertReportPermission("生成月度报表");
		if (month == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "月份不能为空");
		}
		if (!force) {
			MonthlyReport existing = monthlyReportRepository.findByReportMonth(month).orElse(null);
			if (existing != null) {
				log.debug("月度报表已存在，直接返回：{}", month);
				return existing.getId();
			}
		}
		MonthlyReport report = monthlyReportRepository.findByReportMonth(month)
				.orElseGet(MonthlyReport::new);
		rebuild(report, month);
		MonthlyReport saved = monthlyReportRepository.save(report);
		log.info("生成月度报表：月份={}，报表ID={}，订单数={}，总金额={}",
				month, saved.getId(), saved.getOrderCount(), saved.getTotalAmount());
		eventPublisher.publishEvent(new MonthlyReportGeneratedEvent(month, saved.getId()));
		return saved.getId();
	}

	@Override
	@Transactional
	public MonthlyReportVO getMonthly(YearMonth month, Long deptId) {
		assertReportPermission("查询月度报表");
		if (month == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "月份不能为空");
		}
		MonthlyReport report = monthlyReportRepository.findWithItemsByReportMonth(month).orElse(null);
		if (report == null) {
			generateMonthly(month, false);
			report = monthlyReportRepository.findWithItemsByReportMonth(month)
					.orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR,
							"月度报表生成失败：" + month));
		}
		if (deptId == null) {
			return toVO(report, report.getItems());
		}
		return toVO(report, filterItemsByDept(month, deptId, report.getItems()));
	}

	@Override
	@Transactional
	public Resource exportMonthly(YearMonth month, ExportFormat fmt) {
		assertReportPermission("导出月度报表");
		if (fmt == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "导出格式不能为空");
		}
		if (fmt == ExportFormat.PDF) {
			throw new BusinessException(ErrorCode.PARAM_INVALID,
					"暂不支持导出 PDF（未引入 PDF 生成库），请使用 XLSX 或 CSV");
		}
		MonthlyReportVO vo = getMonthly(month, null);
		List<String> header = List.of("菜品", "分类", "数量", "金额");
		List<List<String>> rows = new ArrayList<>();
		for (DishSalesVO item : vo.getItems()) {
			rows.add(List.of(
					blankIfNull(item.getRecipeName()),
					blankIfNull(item.getCategory()),
					item.getQuantity() == null ? "0" : item.getQuantity().toString(),
					item.getAmount() == null ? "0.00" : item.getAmount().toPlainString()));
		}
		rows.add(List.of("合计", "",
				vo.getTotalQuantity() == null ? "0" : vo.getTotalQuantity().toString(),
				vo.getTotalAmount() == null ? "0.00" : vo.getTotalAmount().toPlainString()));
		String sheetName = vo.getMonth() + " 月度销售报表";
		return fmt == ExportFormat.CSV
				? TabularExport.csv(header, rows)
				: TabularExport.xlsx(sheetName, header, rows);
	}

	@Override
	@Transactional
	public void refreshMonthly(YearMonth month) {
		assertReportPermission("刷新月度报表");
		generateMonthly(month, true);
	}

	@Override
	@Transactional
	public List<YearMonth> listReportMonths() {
		assertReportPermission("查询报表月份");
		List<YearMonth> months = new ArrayList<>();
		for (MonthlyReport report : monthlyReportRepository.findAll(Sort.by(Sort.Direction.DESC, "reportMonth"))) {
			if (report.getReportMonth() != null) {
				months.add(report.getReportMonth());
			}
		}
		if (months.isEmpty()) {
			// 一张报表都没有时回退当月，避免前端月份选择器为空
			months.add(YearMonth.now());
		}
		return months;
	}

	/**
	 * 按月份重算报表内容（覆盖写：先清空明细再重新聚合）
	 *
	 * @param report 报表实体（新建或已存在）
	 * @param month  月份
	 */
	private void rebuild(MonthlyReport report, YearMonth month) {
		List<OrderVO> orders = orderReader.readMonth(month);
		Map<String, Aggregate> aggregates = aggregate(orders);
		report.setReportMonth(month);
		report.setOrderCount(orders.size());
		report.setGeneratedAt(LocalDateTime.now());
		report.getItems().clear();
		long totalQuantity = 0L;
		BigDecimal totalAmount = BigDecimal.ZERO;
		for (Aggregate item : aggregates.values()) {
			MonthlyReportItem entity = new MonthlyReportItem();
			entity.setRecipeId(item.recipeId);
			entity.setRecipeName(item.recipeName);
			entity.setCategory(item.category);
			entity.setQuantity(item.quantity);
			entity.setAmount(item.amount);
			report.addItem(entity);
			totalQuantity += item.quantity;
			totalAmount = totalAmount.add(item.amount);
		}
		report.setTotalQuantity(totalQuantity);
		report.setTotalAmount(totalAmount);
	}

	/**
	 * 按菜品聚合订单明细
	 *
	 * @param orders 所选月份的有效订单（含快照明细）
	 * @return 聚合结果，按金额倒序
	 */
	private Map<String, Aggregate> aggregate(List<OrderVO> orders) {
		Map<String, Aggregate> aggregates = new LinkedHashMap<>();
		for (OrderVO order : orders) {
			for (DishSalesVO item : itemsOf(order)) {
				String key = item.getRecipeId() == null
						? "name:" + item.getRecipeName() + "@" + item.getCategory()
						: "id:" + item.getRecipeId();
				Aggregate aggregate = aggregates.computeIfAbsent(key, ignored -> new Aggregate(
						item.getRecipeId(), item.getRecipeName(), item.getCategory()));
				aggregate.quantity += item.getQuantity() == null ? 0L : item.getQuantity();
				aggregate.amount = aggregate.amount.add(item.getAmount() == null
						? BigDecimal.ZERO
						: item.getAmount());
			}
		}
		List<Aggregate> sorted = new ArrayList<>(aggregates.values());
		sorted.sort((left, right) -> right.amount.compareTo(left.amount));
		Map<String, Aggregate> result = new LinkedHashMap<>();
		for (Aggregate item : sorted) {
			result.put(item.recipeName + "@" + item.category + "@" + item.recipeId, item);
		}
		return result;
	}

	/**
	 * 按部门过滤报表明细：只保留该部门员工当月点过的菜品
	 *
	 * @param month  月份
	 * @param deptId 部门ID
	 * @param items  全餐厅明细
	 * @return 过滤后的明细
	 */
	private List<MonthlyReportItem> filterItemsByDept(YearMonth month, Long deptId,
			List<MonthlyReportItem> items) {
		// 注：UserService 目前没有“按部门列员工”的契约方法，这里按部门分页取员工（pageSize 上限 100）；
		// 单部门员工超过 100 人时建议补 UserService.listByDept(deptId) 契约，避免漏算。
		UserQuery employeeQuery = new UserQuery();
		employeeQuery.setDeptId(deptId);
		employeeQuery.setPageSize(PageQuery.MAX_PAGE_SIZE);
		PageResult<UserVO> employeePage = userService.page(employeeQuery);
		Set<Long> employeeIds = new LinkedHashSet<>();
		if (employeePage != null && employeePage.getList() != null) {
			for (UserVO employee : employeePage.getList()) {
				if (employee != null && employee.getUserId() != null) {
					employeeIds.add(employee.getUserId());
				}
			}
		}
		if (employeeIds.isEmpty()) {
			return List.of();
		}
		Set<Long> recipeIds = new LinkedHashSet<>();
		boolean hasUnknownRecipe = false;
		for (OrderVO order : orderReader.readMonth(month, employeeIds)) {
			for (DishSalesVO dish : itemsOf(order)) {
				if (dish.getRecipeId() == null) {
					hasUnknownRecipe = true;
				} else {
					recipeIds.add(dish.getRecipeId());
				}
			}
		}
		List<MonthlyReportItem> filtered = new ArrayList<>();
		for (MonthlyReportItem item : items) {
			if (item.getRecipeId() != null && recipeIds.contains(item.getRecipeId())) {
				filtered.add(item);
			} else if (item.getRecipeId() == null && hasUnknownRecipe) {
				// 历史订单明细缺菜品ID时，以“该部门当月也有无ID菜品”为条件保留
				filtered.add(item);
			}
		}
		return filtered;
	}

	/**
	 * 报表实体 → 视图
	 *
	 * @param report 报表实体
	 * @param items  明细（可能是按部门过滤后的子集）
	 * @return 报表视图
	 */
	private MonthlyReportVO toVO(MonthlyReport report, List<MonthlyReportItem> items) {
		MonthlyReportVO vo = new MonthlyReportVO();
		vo.setReportId(report.getId());
		vo.setMonth(MonthConverter.format(report.getReportMonth()));
		vo.setOrderCount(report.getOrderCount() == null ? 0L : report.getOrderCount().longValue());
		vo.setTotalQuantity(report.getTotalQuantity() == null ? 0L : report.getTotalQuantity());
		vo.setTotalAmount(report.getTotalAmount() == null ? BigDecimal.ZERO : report.getTotalAmount());
		vo.setGeneratedAt(report.getGeneratedAt() == null
				? null
				: report.getGeneratedAt().format(DATE_TIME_FORMATTER));
		List<DishSalesVO> dishItems = new ArrayList<>();
		if (items != null) {
			for (MonthlyReportItem item : items) {
				DishSalesVO dish = new DishSalesVO();
				dish.setRecipeId(item.getRecipeId());
				dish.setRecipeName(item.getRecipeName());
				dish.setCategory(item.getCategory());
				dish.setQuantity(item.getQuantity());
				dish.setAmount(item.getAmount());
				dishItems.add(dish);
			}
		}
		vo.setItems(dishItems);
		return vo;
	}

	/**
	 * 订单实体 → 菜品明细（下单时刻快照）
	 *
	 * @param order 订单视图
	 * @return 菜品明细
	 */
	private List<DishSalesVO> itemsOf(OrderVO order) {
		List<DishSalesVO> items = new ArrayList<>();
		if (order == null || order.getItems() == null) {
			return items;
		}
		for (OrderDetailVO detail : order.getItems()) {
			DishSalesVO item = new DishSalesVO();
			item.setRecipeId(detail.getRecipeId());
			item.setRecipeName(detail.getRecipeName());
			item.setCategory(detail.getCategory());
			item.setQuantity(detail.getQuantity() == null ? 0L : detail.getQuantity().longValue());
			item.setAmount(detail.getAmount() == null ? BigDecimal.ZERO : detail.getAmount());
			items.add(item);
		}
		return items;
	}

	/**
	 * null 安全：导出表格时空字符串比 "null" 更合适
	 *
	 * @param value 原值
	 * @return 空字符串或原值
	 */
	private String blankIfNull(String value) {
		return value == null ? "" : value;
	}

	/**
	 * 校验报表操作权限：财务管理或餐厅经理（或持有报表相关权限点）
	 *
	 * @param action 动作名称，用于中文提示
	 */
	private void assertReportPermission(String action) {
		UserContext context = UserContextHolder.require();
		if (context.hasAnyRole(RoleCodes.FINANCE, RoleCodes.MANAGER)) {
			return;
		}
		if (context.hasPermission(PermCodes.REPORT_VIEW) || context.hasPermission(PermCodes.AUDIT_VIEW)) {
			return;
		}
		throw new BusinessException(ErrorCode.FORBIDDEN, "无权执行该操作：" + action);
	}

	/**
	 * 菜品聚合中间结果
	 */
	private static final class Aggregate
	{
		/**
		 * 菜品ID
		 */
		private final Long recipeId;

		/**
		 * 菜名快照
		 */
		private final String recipeName;

		/**
		 * 分类快照
		 */
		private final String category;

		/**
		 * 累计数量
		 */
		private long quantity;

		/**
		 * 累计金额
		 */
		private BigDecimal amount = BigDecimal.ZERO;

		private Aggregate(Long recipeId, String recipeName, String category) {
			this.recipeId = recipeId;
			this.recipeName = recipeName;
			this.category = category;
		}
	}
}
