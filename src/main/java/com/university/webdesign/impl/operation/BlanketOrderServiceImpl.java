package com.university.webdesign.impl.operation;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.DateRange;
import com.university.webdesign.common.PageQuery;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.domain.operation.DailyStatistics;
import com.university.webdesign.domain.operation.DailyStatisticsItem;
import com.university.webdesign.repository.operation.DailyStatisticsRepository;
import com.university.webdesign.service.menu.RecipeService;
import com.university.webdesign.service.menu.dto.CategoryVO;
import com.university.webdesign.service.operation.BlanketOrderService;
import com.university.webdesign.service.operation.ServiceWindowService;
import com.university.webdesign.service.operation.dto.CategoryStatVO;
import com.university.webdesign.service.operation.dto.CategorySumVO;
import com.university.webdesign.service.operation.dto.DailyStatVO;
import com.university.webdesign.service.operation.dto.PrintFormat;
import com.university.webdesign.service.operation.dto.ServiceWindowVO;
import com.university.webdesign.service.order.OrderQueryService;
import com.university.webdesign.service.order.dto.ExportFormat;
import com.university.webdesign.service.order.dto.OrderBriefVO;
import com.university.webdesign.service.order.dto.OrderDetailVO;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 总括订单服务实现。
 * <p>
 * 对应《对外方法表》4.2 的 {@code BlanketOrderService}：在“订餐截止时间”之后汇总当日有效订单，
 * 按菜品（同时保留分类维度）汇总数量与金额，写入 {@code daily_statistics} 快照供厨房备料与生产单打印。
 * <p>
 * 取数红线：订单数据一律走 {@link OrderQueryService#listValidByDate(LocalDate)}，
 * 分类字典走 {@link RecipeService#listCategories()}，**不直接访问任何订单表**。
 * 事务统一用普通 {@code @Transactional}，查询也不加 {@code readOnly = true}。
 */
@Service
@Transactional
public class BlanketOrderServiceImpl implements BlanketOrderService
{
	/**
	 * 生产单/导出文件的时间戳格式
	 */
	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	/**
	 * 生产单展示的时间格式
	 */
	private static final DateTimeFormatter READABLE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final OrderQueryService orderQueryService;
	private final RecipeService recipeService;
	private final DailyStatisticsRepository dailyStatisticsRepository;
	private final ServiceWindowService serviceWindowService;

	public BlanketOrderServiceImpl(
			OrderQueryService orderQueryService,
			RecipeService recipeService,
			DailyStatisticsRepository dailyStatisticsRepository,
			ServiceWindowService serviceWindowService) {
		this.orderQueryService = orderQueryService;
		this.recipeService = recipeService;
		this.dailyStatisticsRepository = dailyStatisticsRepository;
		this.serviceWindowService = serviceWindowService;
	}

	@Override
	public DailyStatVO aggregate(LocalDate date, boolean force) {
		LocalDate target = requireDate(date);
		// 聚合时机：总括订单只在“订餐截止时间”之后生成（对应 AGENTS.md 第 4 节的业务不变量），
		// 截止前调用一律 42201，避免用尚未收齐的订单算出错误的生产量。
		requireWindowClosed(target);
		if (!force) {
			// 幂等：已有快照直接返回，重复触发（定时任务 + 手动补偿）不会覆盖既有结果
			DailyStatistics existing = dailyStatisticsRepository.findByStatisticsDate(target).orElse(null);
			if (existing != null) {
				return toVO(existing);
			}
		}
		return toVO(rebuild(target));
	}

	@Override
	public List<CategoryStatVO> listAggregated(LocalDate date, Long categoryId) {
		LocalDate target = requireDate(date);
		DailyStatistics snapshot = dailyStatisticsRepository.findByStatisticsDate(target).orElse(null);
		if (snapshot == null) {
			// 只读方法不做隐式写库：尚未聚合时返回空，由 M3-01 / 定时任务负责生成快照
			return new ArrayList<>();
		}
		String categoryName = resolveCategoryName(categoryId);
		List<CategoryStatVO> result = new ArrayList<>();
		if (snapshot.getItems() == null) {
			return result;
		}
		for (DailyStatisticsItem item : snapshot.getItems()) {
			if (categoryName != null && !categoryName.equals(item.getCategory())) {
				continue;
			}
			result.add(toCategoryStatVO(item));
		}
		return result;
	}

	@Override
	public Resource printProductionOrder(LocalDate date, List<Long> categoryIds, PrintFormat fmt) {
		LocalDate target = requireDate(date);
		requireWindowClosed(target);
		DailyStatistics snapshot = dailyStatisticsRepository.findByStatisticsDate(target)
				.orElseGet(() -> rebuild(target));
		if (snapshot.getItems() == null) {
			snapshot.setItems(new ArrayList<>());
		}

		List<String> categoryNames = resolveCategoryNames(categoryIds);
		List<DailyStatisticsItem> items = new ArrayList<>();
		for (DailyStatisticsItem item : snapshot.getItems()) {
			if (categoryNames == null || categoryNames.contains(item.getCategory())) {
				items.add(item);
			}
		}
		PrintFormat format = fmt == null ? PrintFormat.TXT : fmt;
		String baseName = "production-order-" + TIMESTAMP.format(LocalDateTime.now());
		if (format == PrintFormat.XLSX) {
			List<String> headers = List.of("分类", "菜品", "单位", "需求总量", "金额(元)", "涉及订单数");
			List<List<String>> rows = new ArrayList<>();
			for (DailyStatisticsItem item : items) {
				rows.add(Arrays.asList(
						nullSafe(item.getCategory()),
						nullSafe(item.getRecipeName()),
						nullSafe(item.getUnit()),
						String.valueOf(item.getQuantity()),
						amount(item.getAmount()).toPlainString(),
						String.valueOf(item.getOrderCount())));
			}
			return OperationExportSupport.render(ExportFormat.XLSX, baseName, headers, rows);
		}
		if (format == PrintFormat.PDF) {
			throw new BusinessException(com.university.webdesign.common.ErrorCode.PARAM_INVALID,
					"生产单暂不支持 PDF（未引入 PDF 生成库），请使用 TXT / PRINT / XLSX");
		}
		return OperationExportSupport.text(baseName + ".txt", renderProductionOrder(target, items, snapshot));
	}

	@Override
	public List<CategorySumVO> sumByCategory(LocalDate date) {
		Map<String, CategorySumVO> sums = new LinkedHashMap<>();
		for (CategoryStatVO item : listAggregated(date, null)) {
			String key = item.getCategory() == null ? "未分类" : item.getCategory();
			CategorySumVO sum = sums.computeIfAbsent(key, name -> {
				CategorySumVO vo = new CategorySumVO();
				vo.setCategory(item.getCategory());
				return vo;
			});
			sum.setDishCount(sum.getDishCount() + 1);
			sum.setTotalQuantity(sum.getTotalQuantity() + item.getTotalQuantity());
			sum.setTotalAmount(amount(sum.getTotalAmount()).add(amount(item.getTotalAmount())));
		}
		return new ArrayList<>(sums.values());
	}

	@Override
	public PageResult<DailyStatVO> pageDailyStat(DateRange range, PageQuery page) {
		DateRange effective = range == null ? DateRange.unbounded() : range;
		PageQuery paging = page == null ? new PageQuery() : page;
		PageRequest request = PageRequest.of(paging.toSpringPageNumber(), paging.normalizedPageSize());
		Page<DailyStatistics> result = effective.from() != null && effective.to() != null
				? dailyStatisticsRepository.findByStatisticsDateBetweenOrderByStatisticsDateDesc(
						effective.from(), effective.to(), request)
				: dailyStatisticsRepository.findAllByOrderByStatisticsDateDesc(request);
		List<DailyStatVO> list = new ArrayList<>();
		for (DailyStatistics snapshot : result.getContent()) {
			list.add(toVO(snapshot));
		}
		return new PageResult<>(result.getTotalElements(), list);
	}

	@Override
	public void refreshDailyStat(LocalDate date) {
		rebuild(requireDate(date));
	}

	// ------------------------------------------------------------------ 内部方法

	/**
	 * 重算某日快照并落库（幂等：覆盖写，`statistics_date` 唯一索引保证只有一行）
	 * <p>
	 * 实现要点：**必须复用已存在的行**（取到实体后清空明细再重填），
	 * 而不是“先 delete 再 insert”。Hibernate 会把 DELETE 推迟到事务提交前的 flush，
	 * 而 INSERT 是立即执行的，同一事务内换行会直接撞上 `statistics_date` 唯一约束
	 * （实测报 `DataIntegrityViolationException`，被全局异常处理器兜成 50000）。
	 *
	 * @param date 汇总日期
	 * @return 快照实体
	 */
	private DailyStatistics rebuild(LocalDate date) {
		List<OrderBriefVO> orders = orderQueryService.listValidByDate(date);
		DailyStatistics snapshot = dailyStatisticsRepository.findByStatisticsDate(date)
				.orElseGet(DailyStatistics::new);
		snapshot.setStatisticsDate(date);
		snapshot.setStatus(DailyStatistics.STATUS_AGGREGATED);
		snapshot.getItems().clear();

		Map<Long, DailyStatisticsItem> grouped = new LinkedHashMap<>();
		BigDecimal totalAmount = BigDecimal.ZERO;
		for (OrderBriefVO order : orders) {
			totalAmount = totalAmount.add(amount(order.getTotalAmount()));
			if (order.getItems() == null) {
				continue;
			}
			for (OrderDetailVO detail : order.getItems()) {
				Long key = detail.getRecipeId() == null ? -1L : detail.getRecipeId();
				DailyStatisticsItem item = grouped.get(key);
				if (item == null) {
					item = new DailyStatisticsItem();
					item.setCategory(detail.getCategory());
					item.setRecipeId(detail.getRecipeId());
					item.setRecipeName(detail.getRecipeName());
					item.setUnit(detail.getUnit());
					item.setQuantity(0);
					item.setAmount(BigDecimal.ZERO);
					item.setOrderCount(0);
					grouped.put(key, item);
				}
				item.setQuantity(item.getQuantity() + (detail.getQuantity() == null ? 0 : detail.getQuantity()));
				item.setAmount(amount(item.getAmount()).add(amount(detail.getAmount())));
				item.setOrderCount(item.getOrderCount() + 1);
			}
		}
		for (DailyStatisticsItem item : grouped.values()) {
			snapshot.addItem(item);
		}
		snapshot.setTotalOrders(orders.size());
		snapshot.setTotalAmount(totalAmount);

		DailyStatistics saved = dailyStatisticsRepository.save(snapshot);
		dailyStatisticsRepository.flush();
		// save 在极端情况下可能返回 null（脏数据/自定义仓储实现），此时仍返回已组装好的实体
		return saved == null ? snapshot : saved;
	}

	/**
	 * 校验并要求目标日期已过订餐截止时间
	 * <p>
	 * 聚合总括订单、打印生产单都只能在截止后执行：截止前订单还在收集中，
	 * 此时汇总出的生产量必然是错的，一律抛 42201。
	 *
	 * @param date 目标日期
	 */
	private void requireWindowClosed(LocalDate date) {
		ServiceWindowVO window = serviceWindowService.get(date, null, null);
		LocalTime cutoff = window == null ? null : window.getCutoffTime();
		LocalDateTime now = LocalDateTime.now();
		if (date.isAfter(now.toLocalDate())) {
			throw BusinessException.outOfWindow("尚未到 " + date + " 的订餐截止时间，暂不能汇总生产数据");
		}
		if (date.isEqual(now.toLocalDate()) && cutoff != null && now.toLocalTime().isBefore(cutoff)) {
			throw BusinessException.outOfWindow("尚未到订餐截止时间 " + cutoff + "，暂不能汇总生产数据");
		}
	}

	/**
	 * 渲染纯文本生产单
	 *
	 * @param date     汇总日期
	 * @param items    汇总明细
	 * @param snapshot 快照（取有效订单数）
	 * @return 纯文本内容
	 */
	private String renderProductionOrder(LocalDate date, List<DailyStatisticsItem> items, DailyStatistics snapshot) {
		StringBuilder text = new StringBuilder();
		text.append("企业餐厅生产单\n");
		text.append("就餐日期：").append(date).append('\n');
		text.append("有效订单数：").append(snapshot.getTotalOrders()).append('\n');
		text.append("需求总金额：").append(amount(snapshot.getTotalAmount()).toPlainString()).append(" 元\n");
		text.append("生成时间：").append(READABLE_TIME.format(LocalDateTime.now())).append('\n');
		text.append("----------------------------------------\n");
		String category = null;
		for (DailyStatisticsItem item : items) {
			if (!java.util.Objects.equals(category, item.getCategory())) {
				category = item.getCategory();
				text.append('【').append(category == null ? "未分类" : category).append("】\n");
			}
			text.append("  ").append(item.getRecipeName())
					.append(' ').append(item.getQuantity())
					.append(nullSafe(item.getUnit()))
					.append("（金额 ").append(amount(item.getAmount()).toPlainString()).append(" 元）")
					.append('\n');
		}
		if (items.isEmpty()) {
			text.append("（当日无有效需求）\n");
		}
		text.append("----------------------------------------\n");
		return text.toString();
	}

	/**
	 * 分类ID → 分类名；为空时返回 null（表示不过滤）
	 *
	 * @param categoryId 分类ID
	 * @return 分类名，未匹配到或入参为空时返回 null
	 */
	private String resolveCategoryName(Long categoryId) {
		if (categoryId == null) {
			return null;
		}
		List<CategoryVO> categories = recipeService.listCategories();
		if (categories == null) {
			return null;
		}
		for (CategoryVO category : categories) {
			if (categoryId.equals(category.getCategoryId())) {
				return category.getCategoryName();
			}
		}
		return null;
	}

	/**
	 * 分类ID集合 → 分类名集合；入参为空时返回 null（表示不过滤）
	 *
	 * @param categoryIds 分类ID集合
	 * @return 分类名集合；入参为空时返回 null
	 */
	private List<String> resolveCategoryNames(List<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return null;
		}
		List<String> names = new ArrayList<>();
		for (Long categoryId : categoryIds) {
			String name = resolveCategoryName(categoryId);
			if (name != null) {
				names.add(name);
			}
		}
		return names;
	}

	/**
	 * 实体 → 快照视图（转换时补上分类维度条目）
	 *
	 * @param snapshot 快照实体
	 * @return 快照视图
	 */
	private DailyStatVO toVO(DailyStatistics snapshot) {
		DailyStatVO vo = new DailyStatVO();
		vo.setStatisticsId(snapshot.getId());
		vo.setStatisticsDate(snapshot.getStatisticsDate());
		vo.setTotalOrders(snapshot.getTotalOrders());
		vo.setTotalAmount(amount(snapshot.getTotalAmount()));
		vo.setStatus(snapshot.getStatus());
		if (snapshot.getGeneratedAt() != null) {
			vo.setGeneratedTime(snapshot.getGeneratedAt()
					.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
		}
		LocalDateTime refreshed = snapshot.getRefreshedAt() == null
				? snapshot.getGeneratedAt() : snapshot.getRefreshedAt();
		if (refreshed != null) {
			vo.setRefreshedTime(refreshed.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
		}
		List<CategoryStatVO> items = new ArrayList<>();
		for (DailyStatisticsItem item : snapshot.getItems()) {
			items.add(toCategoryStatVO(item));
		}
		vo.setItems(items);
		return vo;
	}

	private CategoryStatVO toCategoryStatVO(DailyStatisticsItem item) {
		CategoryStatVO vo = new CategoryStatVO();
		vo.setCategory(item.getCategory());
		vo.setRecipeId(item.getRecipeId());
		vo.setRecipeName(item.getRecipeName());
		vo.setUnit(item.getUnit());
		vo.setTotalQuantity(item.getQuantity());
		vo.setTotalAmount(amount(item.getAmount()));
		vo.setOrderCount(item.getOrderCount());
		return vo;
	}

	private LocalDate requireDate(LocalDate date) {
		return date == null ? LocalDate.now() : date;
	}

	private BigDecimal amount(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private String nullSafe(String value) {
		return value == null ? "" : value;
	}
}
