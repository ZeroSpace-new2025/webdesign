package com.university.webdesign.service.impl.operation;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.PageQuery;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.domain.operation.DeliveryTask;
import com.university.webdesign.domain.operation.DeliveryTaskItem;
import com.university.webdesign.event.DeliveryGeneratedEvent;
import com.university.webdesign.repository.operation.DeliveryTaskRepository;
import com.university.webdesign.service.operation.DeliveryService;
import com.university.webdesign.service.operation.ServiceWindowService;
import com.university.webdesign.service.operation.dto.DeliveryItemVO;
import com.university.webdesign.service.operation.dto.DeliveryQuery;
import com.university.webdesign.service.operation.dto.DeliveryTaskVO;
import com.university.webdesign.service.operation.dto.PrintBatchVO;
import com.university.webdesign.service.operation.dto.ServiceWindowVO;
import com.university.webdesign.service.operation.dto.TaskStatus;
import com.university.webdesign.service.order.OrderQueryService;
import com.university.webdesign.service.order.dto.ExportFormat;
import com.university.webdesign.service.order.dto.OrderBriefVO;
import com.university.webdesign.service.order.dto.OrderDetailVO;
import com.university.webdesign.service.user.UserService;
import com.university.webdesign.service.user.dto.UserBriefVO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 配送服务实现。
 * <p>
 * 对应《对外方法表》4.2 的 {@code DeliveryService}：到达“配餐开始时间”（默认 11:30）后开放，
 * 按员工维度拆分当日有效订单生成配送单，含菜名、分量、工位、电话。
 * <p>
 * 红线：订单数据走 {@link OrderQueryService#listValidByDate(LocalDate)}，
 * 员工姓名/工位/电话走 {@link UserService#listByIds(java.util.Collection)}，
 * 时间窗口走 {@link ServiceWindowService#get(LocalDate, String, Long)}，
 * **不直接访问订单表或用户表**。事务统一用普通 {@code @Transactional}。
 */
@Service
@Transactional
public class DeliveryServiceImpl implements DeliveryService
{
	/**
	 * 任务编号中的日期格式
	 */
	private static final DateTimeFormatter TASK_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

	/**
	 * 打印批次号格式
	 */
	private static final DateTimeFormatter BATCH_NO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	/**
	 * 可读时间格式
	 */
	private static final DateTimeFormatter READABLE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final DeliveryTaskRepository deliveryTaskRepository;
	private final OrderQueryService orderQueryService;
	private final UserService userService;
	private final ServiceWindowService serviceWindowService;
	private final ApplicationEventPublisher eventPublisher;

	public DeliveryServiceImpl(
			DeliveryTaskRepository deliveryTaskRepository,
			OrderQueryService orderQueryService,
			UserService userService,
			ServiceWindowService serviceWindowService,
			ApplicationEventPublisher eventPublisher) {
		this.deliveryTaskRepository = deliveryTaskRepository;
		this.orderQueryService = orderQueryService;
		this.userService = userService;
		this.serviceWindowService = serviceWindowService;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public List<Long> generateTasks(LocalDate date, Long deptId, String workstation) {
		LocalDate target = date == null ? LocalDate.now() : date;
		ServiceWindowVO window = serviceWindowService.get(target, null, deptId);
		requireDeliveryOpen(target, window);

		List<OrderBriefVO> orders = orderQueryService.listValidByDate(target);
		if (orders.isEmpty()) {
			eventPublisher.publishEvent(new DeliveryGeneratedEvent(target, List.of(), 0));
			return new ArrayList<>();
		}

		Map<Long, UserBriefVO> briefs = loadBriefs(orders);
		Map<Long, List<OrderBriefVO>> grouped = groupByEmployee(orders, briefs, deptId, workstation);
		if (grouped.isEmpty()) {
			eventPublisher.publishEvent(new DeliveryGeneratedEvent(target, List.of(), 0));
			return new ArrayList<>();
		}

		// 不重复派单：已存在任务的员工直接跳过（数据库 (statistics_date, employee_id) 唯一约束兜底）
		Set<Long> existing = new LinkedHashSet<>();
		for (DeliveryTask task : deliveryTaskRepository.findByStatisticsDateAndEmployeeIdIn(
				target, new ArrayList<>(grouped.keySet()))) {
			existing.add(task.getEmployeeId());
		}

		long sequence = deliveryTaskRepository.countByStatisticsDate(target);
		List<Long> created = new ArrayList<>();
		for (Map.Entry<Long, List<OrderBriefVO>> entry : grouped.entrySet()) {
			if (existing.contains(entry.getKey())) {
				continue;
			}
			sequence = sequence + 1;
			DeliveryTask task = buildTask(target, entry.getKey(), briefs.get(entry.getKey()),
					entry.getValue(), window, sequence);
			created.add(deliveryTaskRepository.save(task).getId());
		}
		eventPublisher.publishEvent(new DeliveryGeneratedEvent(target, created, created.size()));
		return created;
	}

	@Override
	public PageResult<DeliveryTaskVO> page(DeliveryQuery query) {
		DeliveryQuery effective = query == null ? new DeliveryQuery() : query;
		// 不强制要求日期：管理视图需要能分页查看全部配送任务，
		// 只按状态/工位/部门筛选也应当被支持。
		PageRequest request = PageRequest.of(effective.toSpringPageNumber(), effective.normalizedPageSize(),
				Sort.by(Sort.Direction.ASC, "taskNo"));
		// 全部筛选条件（含 employeeId）都下推到数据库，保证分页与 total 一致
		Page<DeliveryTask> result = deliveryTaskRepository.findAll(buildSpecification(effective), request);
		List<DeliveryTaskVO> list = new ArrayList<>();
		for (DeliveryTask task : result.getContent()) {
			list.add(toVO(task));
		}
		enrich(list);
		return new PageResult<>(result.getTotalElements(), list);
	}

	@Override
	public DeliveryTaskVO getDetail(Long taskId) {
		DeliveryTask task = requireTask(taskId);
		DeliveryTaskVO vo = toVO(task);
		enrich(List.of(vo));
		return vo;
	}

	@Override
	public PrintBatchVO batchPrint(List<Long> taskIds, String templateId) {
		if (taskIds == null || taskIds.isEmpty()) {
			throw BusinessException.paramInvalid("请选择要打印的配送任务");
		}
		List<DeliveryTask> tasks = deliveryTaskRepository.findAllById(taskIds);
		if (tasks.isEmpty()) {
			throw BusinessException.notFound("没有找到可打印的配送任务");
		}
		if (tasks.size() != new LinkedHashSet<>(taskIds).size()) {
			throw BusinessException.notFound("部分配送任务不存在，请刷新后重试");
		}

		LocalDateTime now = LocalDateTime.now();
		for (DeliveryTask task : tasks) {
			// 累加打印次数并记录打印时间，便于审计“谁在什么时候重复打印过”
			task.markPrinted(now);
		}
		deliveryTaskRepository.saveAll(tasks);

		PrintBatchVO vo = new PrintBatchVO();
		vo.setPrintedCount(tasks.size());
		vo.setPrintBatchId("PB-" + BATCH_NO.format(now) + "-" + tasks.size());
		vo.setContent(renderBatch(tasks, templateId));
		return vo;
	}

	@Override
	public void updateStatus(Long taskId, TaskStatus status, String receiver, String remark) {
		DeliveryTask task = requireTask(taskId);
		if (status == null) {
			throw BusinessException.paramInvalid("目标状态不能为空");
		}
		TaskStatus current = task.getStatus() == null ? TaskStatus.PENDING : task.getStatus();
		if (!current.canTransitionTo(status)) {
			throw new BusinessException(com.university.webdesign.common.ErrorCode.STATE_CONFLICT,
					"配送状态不允许从 " + current.getText() + " 流转到 " + status.getText());
		}
		task.setStatus(status);
		if (receiver != null && !receiver.isBlank()) {
			task.setReceiver(receiver);
		}
		if (remark != null && !remark.isBlank()) {
			task.setRemark(remark);
		}
		if (status == TaskStatus.DELIVERED || status == TaskStatus.EXCEPTION) {
			task.setDeliveredAt(LocalDateTime.now());
		}
		deliveryTaskRepository.save(task);
	}

	@Override
	public Resource export(LocalDate date, Long deptId, ExportFormat fmt) {
		LocalDate target = date == null ? LocalDate.now() : date;
		List<DeliveryTask> tasks = deliveryTaskRepository.findByStatisticsDateOrderByTaskNoAsc(target);
		List<String> headers = List.of("任务编号", "就餐日期", "员工姓名", "员工ID", "部门ID", "工位", "电话",
				"配送明细", "状态", "打印次数", "最近打印时间", "签收人", "备注", "送达时间");
		List<List<String>> rows = new ArrayList<>();
		Map<Long, UserBriefVO> briefs = loadBriefsFromTasks(tasks);
		for (DeliveryTask task : tasks) {
			if (deptId != null && !deptId.equals(task.getDeptId())) {
				continue;
			}
			UserBriefVO brief = briefs.get(task.getEmployeeId());
			rows.add(List.of(
					nullSafe(task.getTaskNo()),
					String.valueOf(task.getStatisticsDate()),
					brief == null ? "" : nullSafe(brief.name()),
					String.valueOf(task.getEmployeeId()),
					task.getDeptId() == null ? "" : String.valueOf(task.getDeptId()),
					nullSafe(task.getWorkstation()),
					nullSafe(task.getPhone()),
					summarizeItems(task.getItems()),
					task.getStatus() == null ? "" : task.getStatus().getText(),
					String.valueOf(task.getPrintCount()),
					formatTime(task.getPrintedAt()),
					nullSafe(task.getReceiver()),
					nullSafe(task.getRemark()),
					formatTime(task.getDeliveredAt())));
		}
		return OperationExportSupport.render(fmt, "delivery-ledger-" + target, headers, rows);
	}

	// ------------------------------------------------------------------ 内部方法

	/**
	 * 校验已到配餐开始时间（未到抛 42201）
	 *
	 * @param date   配送日期
	 * @param window 生效的时间窗口
	 */
	private void requireDeliveryOpen(LocalDate date, ServiceWindowVO window) {
		if (date.isAfter(LocalDate.now())) {
			throw BusinessException.outOfWindow("尚未到 " + date + " 的配餐开始时间，暂不能生成配送单");
		}
		if (!date.isEqual(LocalDate.now())) {
			// 历史日期已过配餐时间，允许补生成
			return;
		}
		LocalTime start = window == null ? null : window.getDeliveryStartTime();
		if (start != null && LocalDateTime.now().toLocalTime().isBefore(start)) {
			throw BusinessException.outOfWindow("尚未到配餐开始时间 " + start + "，暂不能生成配送单");
		}
	}

	/**
	 * 按员工ID分组当日订单，并按部门/工位过滤
	 *
	 * @param orders      当日有效订单
	 * @param briefs      员工信息
	 * @param deptId      限定部门，可为空
	 * @param workstation 限定工位，可为空
	 * @return 员工ID → 该员工的订单列表
	 */
	private Map<Long, List<OrderBriefVO>> groupByEmployee(List<OrderBriefVO> orders, Map<Long, UserBriefVO> briefs,
			Long deptId, String workstation) {
		Map<Long, List<OrderBriefVO>> grouped = new LinkedHashMap<>();
		for (OrderBriefVO order : orders) {
			Long employeeId = order.getEmployeeId();
			if (employeeId == null) {
				continue;
			}
			UserBriefVO brief = briefs.get(employeeId);
			if (deptId != null && (brief == null || !deptId.equals(brief.deptId()))) {
				continue;
			}
			String targetWorkstation = brief == null ? null : brief.workstation();
			if (workstation != null && !workstation.isBlank()
					&& (targetWorkstation == null || !workstation.equals(targetWorkstation))) {
				continue;
			}
			grouped.computeIfAbsent(employeeId, key -> new ArrayList<>()).add(order);
		}
		return grouped;
	}

	/**
	 * 组装配送任务实体（明细取订单详情快照的 quantity / unit）
	 *
	 * @param date       就餐日期
	 * @param employeeId 员工ID
	 * @param brief      员工信息，可为空
	 * @param orders     该员工的订单
	 * @param window     时间窗口（取部门归属兜底）
	 * @param sequence   当日任务流水
	 * @return 任务实体
	 */
	private DeliveryTask buildTask(LocalDate date, Long employeeId, UserBriefVO brief,
			List<OrderBriefVO> orders, ServiceWindowVO window, long sequence) {
		DeliveryTask task = new DeliveryTask();
		Long deptId = brief == null ? null : brief.deptId();
		task.setTaskNo("DT-" + TASK_NO_DATE.format(date) + "-" + sequence);
		task.setStatisticsDate(date);
		task.setEmployeeId(employeeId);
		task.setDeptId(deptId);
		task.setWorkstation(brief == null ? null : brief.workstation());
		task.setPhone(brief == null ? null : brief.phone());
		task.setStatus(TaskStatus.PENDING);
		Map<Long, DeliveryTaskItem> merged = new LinkedHashMap<>();
		for (OrderBriefVO order : orders) {
			if (order.getItems() == null) {
				continue;
			}
			for (OrderDetailVO detail : order.getItems()) {
				Long key = detail.getRecipeId() == null ? -1L : detail.getRecipeId();
				DeliveryTaskItem item = merged.get(key);
				if (item == null) {
					item = new DeliveryTaskItem(detail.getRecipeId(), detail.getRecipeName(),
							detail.getQuantity(), detail.getUnit());
					merged.put(key, item);
				} else {
					// 同一员工当天多张订单（历史数据）时合并同菜品分量
					item.setQuantity((item.getQuantity() == null ? 0 : item.getQuantity())
							+ (detail.getQuantity() == null ? 0 : detail.getQuantity()));
				}
			}
		}
		for (DeliveryTaskItem item : merged.values()) {
			task.addItem(item);
		}
		return task;
	}

	/**
	 * 批量补齐员工信息，避免逐条查询
	 *
	 * @param orders 订单列表
	 * @return 员工ID → 员工信息
	 */
	private Map<Long, UserBriefVO> loadBriefs(List<OrderBriefVO> orders) {
		Set<Long> ids = new LinkedHashSet<>();
		for (OrderBriefVO order : orders) {
			if (order.getEmployeeId() != null) {
				ids.add(order.getEmployeeId());
			}
		}
		return queryBriefs(ids);
	}

	private Map<Long, UserBriefVO> loadBriefsFromTasks(List<DeliveryTask> tasks) {
		Set<Long> ids = new LinkedHashSet<>();
		for (DeliveryTask task : tasks) {
			if (task.getEmployeeId() != null) {
				ids.add(task.getEmployeeId());
			}
		}
		return queryBriefs(ids);
	}

	private Map<Long, UserBriefVO> queryBriefs(Set<Long> ids) {
		Map<Long, UserBriefVO> briefs = new LinkedHashMap<>();
		if (ids.isEmpty()) {
			return briefs;
		}
		List<UserBriefVO> found = userService.listByIds(ids);
		if (found == null) {
			return briefs;
		}
		for (UserBriefVO brief : found) {
			if (brief != null && brief.userId() != null) {
				briefs.put(brief.userId(), brief);
			}
		}
		return briefs;
	}

	/**
	 * 补齐配送单视图中的员工姓名与工位/电话（任务未存到值时用用户中心的实时值兜底）
	 *
	 * @param tasks 任务视图列表
	 */
	private void enrich(List<DeliveryTaskVO> tasks) {
		Set<Long> ids = new LinkedHashSet<>();
		for (DeliveryTaskVO task : tasks) {
			if (task.getEmployeeId() != null) {
				ids.add(task.getEmployeeId());
			}
		}
		Map<Long, UserBriefVO> briefs = queryBriefs(ids);
		for (DeliveryTaskVO task : tasks) {
			UserBriefVO brief = briefs.get(task.getEmployeeId());
			if (brief == null) {
				continue;
			}
			task.setEmployeeName(brief.name());
			if (task.getDeptId() == null) {
				task.setDeptId(brief.deptId());
			}
			task.setDeptName(brief.deptName());
			if (task.getWorkstation() == null) {
				task.setWorkstation(brief.workstation());
			}
			if (task.getPhone() == null) {
				task.setPhone(brief.phone());
			}
		}
	}

	/**
	 * 渲染批量打印内容（纯文本，供 `PRINT` 格式直接推送打印机）
	 *
	 * @param tasks      任务列表
	 * @param templateId 打印模板标识，为空取默认模板
	 * @return 打印内容
	 */
	private String renderBatch(List<DeliveryTask> tasks, String templateId) {
		StringBuilder text = new StringBuilder();
		text.append("企业餐厅配送单").append('\n');
		text.append("打印批次模板：").append(templateId == null || templateId.isBlank() ? "DEFAULT" : templateId)
				.append('\n');
		text.append("打印时间：").append(READABLE_TIME.format(LocalDateTime.now())).append('\n');
		for (DeliveryTask task : tasks) {
			text.append("========================================\n");
			text.append("任务编号：").append(nullSafe(task.getTaskNo())).append('\n');
			text.append("就餐日期：").append(task.getStatisticsDate()).append('\n');
			text.append("员工ID：").append(task.getEmployeeId()).append('\n');
			text.append("工位：").append(nullSafe(task.getWorkstation())).append('\n');
			text.append("电话：").append(nullSafe(task.getPhone())).append('\n');
			text.append("状态：").append(task.getStatus() == null ? "" : task.getStatus().getText()).append('\n');
			text.append("明细：\n");
			for (DeliveryTaskItem item : task.getItems()) {
				text.append("  ").append(item.getRecipeName())
						.append(' ').append(item.getQuantity() == null ? "" : item.getQuantity())
						.append(nullSafe(item.getUnit())).append('\n');
			}
		}
		text.append("========================================\n");
		text.append("共 ").append(tasks.size()).append(" 张配送单\n");
		return text.toString();
	}

	private DeliveryTask requireTask(Long taskId) {
		if (taskId == null) {
			throw BusinessException.paramInvalid("配送任务ID不能为空");
		}
		return deliveryTaskRepository.findById(taskId)
				.orElseThrow(() -> BusinessException.notFound("配送任务不存在：" + taskId));
	}

	/**
	 * 组装配送任务分页查询条件（日期 / 部门 / 工位 / 员工 / 状态）
	 *
	 * @param query 查询条件
	 * @return JPA Specification
	 */
	private Specification<DeliveryTask> buildSpecification(DeliveryQuery query) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			// 日期只在传了才作为筛选条件：无条件加入 `statisticsDate = null` 会让
			// “不传日期查全部”永远返回空列表（实测 page 返回 total=0，而详情能查到数据）。
			if (query.getDate() != null) {
				predicates.add(criteriaBuilder.equal(root.get("statisticsDate"), query.getDate()));
			}
			if (query.getDeptId() != null) {
				predicates.add(criteriaBuilder.equal(root.get("deptId"), query.getDeptId()));
			}
			if (query.getEmployeeId() != null) {
				predicates.add(criteriaBuilder.equal(root.get("employeeId"), query.getEmployeeId()));
			}
			if (query.getWorkstation() != null && !query.getWorkstation().isBlank()) {
				predicates.add(criteriaBuilder.equal(root.get("workstation"), query.getWorkstation()));
			}
			if (query.getStatus() != null && !query.getStatus().isBlank()) {
				predicates.add(criteriaBuilder.equal(root.get("status"), TaskStatus.parse(query.getStatus())));
			}
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}

	/**
	 * 实体 → 任务视图
	 *
	 * @param task 任务实体
	 * @return 任务视图
	 */
	private DeliveryTaskVO toVO(DeliveryTask task) {
		DeliveryTaskVO vo = new DeliveryTaskVO();
		vo.setTaskId(task.getId());
		vo.setTaskNo(task.getTaskNo());
		vo.setDeliveryDate(task.getStatisticsDate());
		vo.setEmployeeId(task.getEmployeeId());
		vo.setDeptId(task.getDeptId());
		vo.setWorkstation(task.getWorkstation());
		vo.setPhone(task.getPhone());
		vo.setStatus(task.getStatus() == null ? null : task.getStatus().name());
		vo.setStatusText(task.getStatus() == null ? null : task.getStatus().getText());
		vo.setPrintCount(task.getPrintCount());
		vo.setPrintedAt(formatTime(task.getPrintedAt()));
		vo.setReceiver(task.getReceiver());
		vo.setRemark(task.getRemark());
		vo.setDeliveredAt(formatTime(task.getDeliveredAt()));
		List<DeliveryItemVO> items = new ArrayList<>();
		for (DeliveryTaskItem item : task.getItems()) {
			items.add(new DeliveryItemVO(item.getRecipeId(), item.getRecipeName(),
					item.getQuantity(), item.getUnit()));
		}
		vo.setItems(items);
		return vo;
	}

	private String summarizeItems(List<DeliveryTaskItem> items) {
		StringBuilder text = new StringBuilder();
		for (DeliveryTaskItem item : items) {
			if (text.length() > 0) {
				text.append('；');
			}
			text.append(item.getRecipeName())
					.append(' ')
					.append(item.getQuantity() == null ? "" : item.getQuantity())
					.append(nullSafe(item.getUnit()));
		}
		return text.toString();
	}

	private String formatTime(LocalDateTime time) {
		return time == null ? "" : READABLE_TIME.format(time);
	}

	private String nullSafe(String value) {
		return value == null ? "" : value;
	}
}
