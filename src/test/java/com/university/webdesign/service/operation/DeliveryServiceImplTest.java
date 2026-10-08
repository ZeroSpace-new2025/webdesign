package com.university.webdesign.service.operation;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.domain.operation.DeliveryTask;
import com.university.webdesign.domain.operation.DeliveryTaskItem;
import com.university.webdesign.event.DeliveryGeneratedEvent;
import com.university.webdesign.repository.operation.DeliveryTaskRepository;
import com.university.webdesign.service.impl.operation.DeliveryServiceImpl;
import com.university.webdesign.service.operation.dto.DeliveryQuery;
import com.university.webdesign.service.operation.dto.DeliveryTaskVO;
import com.university.webdesign.service.operation.dto.PrintBatchVO;
import com.university.webdesign.service.operation.dto.ServiceWindowVO;
import com.university.webdesign.service.operation.dto.TaskStatus;
import com.university.webdesign.service.order.OrderQueryService;
import com.university.webdesign.service.order.dto.OrderBriefVO;
import com.university.webdesign.service.order.dto.OrderDetailVO;
import com.university.webdesign.service.user.UserService;
import com.university.webdesign.service.user.dto.UserBriefVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 配送服务单元测试。
 * <p>
 * 用 Mockito 直接驱动 {@link DeliveryServiceImpl}：订单取自
 * {@code OrderQueryService.listValidByDate}、员工信息取自 {@code UserService.listByIds}、
 * 时间窗口取自 {@code ServiceWindowService.get}，覆盖未到配餐时间的 42201、
 * 按员工拆单、不重复派单、状态机与批量打印次数累加。
 */
class DeliveryServiceImplTest
{
	private final DeliveryTaskRepository deliveryTaskRepository = mock(DeliveryTaskRepository.class);

	private final OrderQueryService orderQueryService = mock(OrderQueryService.class);

	private final UserService userService = mock(UserService.class);

	private final com.university.webdesign.service.operation.ServiceWindowService serviceWindowService =
			mock(com.university.webdesign.service.operation.ServiceWindowService.class);

	private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

	private final DeliveryServiceImpl service = new DeliveryServiceImpl(
			deliveryTaskRepository, orderQueryService, userService, serviceWindowService, eventPublisher);

	@Test
	@DisplayName("未到配餐开始时间时抛 42201，不派单")
	void generateRejectsBeforeDeliveryStart() {
		LocalDate today = LocalDate.now();
		when(serviceWindowService.get(today, null, null)).thenReturn(window(LocalTime.of(23, 59)));

		assertThatThrownBy(() -> service.generateTasks(today, null, null))
				.isInstanceOf(BusinessException.class)
				.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
						.isEqualTo(ErrorCode.OUT_OF_TIME_WINDOW));
		verify(deliveryTaskRepository, never()).save(any());
	}

	@Test
	@DisplayName("未来日期不允许提前生成配送任务")
	void generateRejectsFutureDate() {
		LocalDate tomorrow = LocalDate.now().plusDays(1);
		when(serviceWindowService.get(tomorrow, null, null)).thenReturn(window(LocalTime.of(11, 30)));

		assertThatThrownBy(() -> service.generateTasks(tomorrow, null, null))
				.isInstanceOf(BusinessException.class)
				.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
						.isEqualTo(ErrorCode.OUT_OF_TIME_WINDOW));
	}

	@Test
	@DisplayName("按员工拆单：明细取订单快照的 quantity/unit，任务号形如 DT-yyyyMMdd-1")
	void generateCreatesOneTaskPerEmployee() {
		LocalDate today = LocalDate.now();
		// 用 00:00 表示“一定已到配餐开始时间”，避免测试因运行时刻（如凌晨）而偶发失败
		when(serviceWindowService.get(today, null, null)).thenReturn(window(LocalTime.of(0, 1)));
		when(orderQueryService.listValidByDate(today)).thenReturn(List.of(
				order(1L, 201L, detail(101L, "小炒肉", "份", 2), detail(103L, "米饭", "两", 1)),
				order(2L, 202L, detail(101L, "小炒肉", "份", 1))));
		when(userService.listByIds(anyCollection())).thenReturn(List.of(
				brief(201L, "张三", 7L, "A区-01", "13800000001"),
				brief(202L, "李四", 7L, "A区-02", "13800000002")));
		when(deliveryTaskRepository.findByStatisticsDateAndEmployeeIdIn(any(), anyCollection()))
				.thenReturn(List.of());
		when(deliveryTaskRepository.countByStatisticsDate(today)).thenReturn(0L);
		when(deliveryTaskRepository.save(any(DeliveryTask.class))).thenAnswer(invocation -> {
			DeliveryTask task = invocation.getArgument(0);
			task.setId(task.getEmployeeId());
			return task;
		});

		List<Long> ids = service.generateTasks(today, null, null);

		assertThat(ids).containsExactly(201L, 202L);
		ArgumentCaptor<DeliveryTask> captor = ArgumentCaptor.forClass(DeliveryTask.class);
		verify(deliveryTaskRepository, org.mockito.Mockito.times(2)).save(captor.capture());
		DeliveryTask first = captor.getAllValues().get(0);
		assertThat(first.getTaskNo()).isEqualTo("DT-" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
				.format(today) + "-1");
		assertThat(first.getStatisticsDate()).isEqualTo(today);
		assertThat(first.getWorkstation()).isEqualTo("A区-01");
		assertThat(first.getPhone()).isEqualTo("13800000001");
		assertThat(first.getDeptId()).isEqualTo(7L);
		assertThat(first.getStatus()).isEqualTo(TaskStatus.PENDING);
		assertThat(first.getItems()).extracting(DeliveryTaskItem::getRecipeName)
				.containsExactly("小炒肉", "米饭");
		assertThat(first.getItems().get(0).getQuantity()).isEqualTo(2);
		assertThat(first.getItems().get(0).getUnit()).isEqualTo("份");
		verify(eventPublisher).publishEvent(any(DeliveryGeneratedEvent.class));
	}

	@Test
	@DisplayName("不重复派单：已有任务的员工被跳过")
	void generateSkipsExistingTasks() {
		LocalDate today = LocalDate.now();
		when(serviceWindowService.get(today, null, null)).thenReturn(window(LocalTime.of(0, 1)));
		when(orderQueryService.listValidByDate(today)).thenReturn(List.of(
				order(1L, 201L, detail(101L, "小炒肉", "份", 1))));
		when(userService.listByIds(anyCollection())).thenReturn(List.of(
				brief(201L, "张三", 7L, "A区-01", "13800000001")));
		DeliveryTask existing = new DeliveryTask();
		existing.setId(88L);
		existing.setEmployeeId(201L);
		existing.setStatisticsDate(today);
		when(deliveryTaskRepository.findByStatisticsDateAndEmployeeIdIn(eq(today), anyCollection()))
				.thenReturn(List.of(existing));

		List<Long> ids = service.generateTasks(today, null, null);

		assertThat(ids).isEmpty();
		verify(deliveryTaskRepository, never()).save(any());
	}

	@Test
	@DisplayName("批量打印：累加 print_count、写 printed_at，批次号形如 PB-yyyyMMddHHmmss-n")
	void batchPrintIncrementsPrintCount() {
		DeliveryTask task = new DeliveryTask();
		task.setId(1L);
		task.setTaskNo("DT-20260101-1");
		task.setEmployeeId(201L);
		task.setStatus(TaskStatus.DELIVERING);
		when(deliveryTaskRepository.findAllById(List.of(1L))).thenReturn(List.of(task));

		PrintBatchVO batch = service.batchPrint(List.of(1L), null);

		assertThat(batch.getPrintedCount()).isEqualTo(1);
		assertThat(batch.getPrintBatchId()).startsWith("PB-").endsWith("-1");
		assertThat(batch.getContent()).contains("企业餐厅配送单").contains("DEFAULT");
		assertThat(task.getPrintCount()).isEqualTo(1);
		assertThat(task.getPrintedAt()).isNotNull();
	}

	@Test
	@DisplayName("批量打印：任务列表为空时抛 40001")
	void batchPrintRejectsEmptySelection() {
		assertThatThrownBy(() -> service.batchPrint(List.of(), null))
				.isInstanceOf(BusinessException.class)
				.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
						.isEqualTo(ErrorCode.PARAM_INVALID));
	}

	@Test
	@DisplayName("状态流转：PENDING → DELIVERING → DELIVERED 合法")
	void updateStatusAllowsLegalTransition() {
		DeliveryTask task = task(1L, TaskStatus.PENDING);
		when(deliveryTaskRepository.findById(1L)).thenReturn(Optional.of(task));

		service.updateStatus(1L, TaskStatus.DELIVERING, null, null);
		assertThat(task.getStatus()).isEqualTo(TaskStatus.DELIVERING);

		service.updateStatus(1L, TaskStatus.DELIVERED, "张三", "已放置前台");
		assertThat(task.getStatus()).isEqualTo(TaskStatus.DELIVERED);
		assertThat(task.getReceiver()).isEqualTo("张三");
		assertThat(task.getRemark()).isEqualTo("已放置前台");
		assertThat(task.getDeliveredAt()).isNotNull();
	}

	@Test
	@DisplayName("状态流转：已送达不可回退，抛 40902")
	void updateStatusRejectsIllegalTransition() {
		DeliveryTask task = task(2L, TaskStatus.DELIVERED);
		when(deliveryTaskRepository.findById(2L)).thenReturn(Optional.of(task));

		assertThatThrownBy(() -> service.updateStatus(2L, TaskStatus.DELIVERING, null, null))
				.isInstanceOf(BusinessException.class)
				.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
						.isEqualTo(ErrorCode.STATE_CONFLICT));
		verify(deliveryTaskRepository, never()).save(any());
	}

	@Test
	@DisplayName("详情：任务不存在抛 40400")
	void detailMissingTask() {
		when(deliveryTaskRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getDetail(404L))
				.isInstanceOf(BusinessException.class)
				.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
						.isEqualTo(ErrorCode.NOT_FOUND));
	}

	@Test
	@DisplayName("详情：补齐员工姓名与部门")
	void detailEnrichesEmployee() {
		DeliveryTask task = task(3L, TaskStatus.PENDING);
		task.setEmployeeId(201L);
		when(deliveryTaskRepository.findById(3L)).thenReturn(Optional.of(task));
		when(userService.listByIds(anyCollection())).thenReturn(List.of(
				brief(201L, "张三", 7L, "A区-01", "13800000001")));

		DeliveryTaskVO vo = service.getDetail(3L);

		assertThat(vo.getEmployeeName()).isEqualTo("张三");
		assertThat(vo.getDeptName()).isEqualTo("研发部");
		assertThat(vo.getWorkstation()).isEqualTo("A区-01");
		assertThat(vo.getStatusText()).isEqualTo("待配送");
	}

	@Test
	@DisplayName("分页查询：日期不是必填，缺失时返回全部分页而非报错")
	void pageAllowsMissingDate() {
		DeliveryQuery query = new DeliveryQuery();
		when(deliveryTaskRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
				any(org.springframework.data.domain.Pageable.class)))
				.thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

		PageResult<DeliveryTaskVO> result = service.page(query);

		assertThat(result.getTotal()).isZero();
		assertThat(result.getList()).isEmpty();
	}

	@Test
	@DisplayName("分页查询：条件全部下推数据库，返回分页总数与视图")
	void pagePushesConditionsDown() {
		DeliveryQuery query = new DeliveryQuery();
		query.setDate(LocalDate.of(2026, 1, 1));
		query.setEmployeeId(201L);
		DeliveryTask task = task(1L, TaskStatus.PENDING);
		task.setEmployeeId(201L);
		when(deliveryTaskRepository.findAll(any(Specification.class), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(task), PageRequest.of(0, 20), 1));
		when(userService.listByIds(anyCollection())).thenReturn(List.of(
				brief(201L, "张三", 7L, "A区-01", "13800000001")));

		PageResult<DeliveryTaskVO> result = service.page(query);

		assertThat(result.getTotal()).isEqualTo(1);
		assertThat(result.getList()).hasSize(1);
		assertThat(result.getList().get(0).getEmployeeName()).isEqualTo("张三");
	}

	// ------------------------------------------------------------------ 测试数据

	private DeliveryTask task(Long id, TaskStatus status) {
		DeliveryTask task = new DeliveryTask();
		task.setId(id);
		task.setTaskNo("DT-20260101-" + id);
		task.setStatisticsDate(LocalDate.of(2026, 1, 1));
		task.setEmployeeId(200L + id);
		task.setStatus(status);
		task.setItems(new ArrayList<>(List.of(new DeliveryTaskItem(101L, "小炒肉", 2, "份"))));
		return task;
	}

	private OrderBriefVO order(Long orderId, Long employeeId, OrderDetailVO... details) {
		OrderBriefVO brief = new OrderBriefVO();
		brief.setOrderId(orderId);
		brief.setOrderNo("20260101" + orderId);
		brief.setEmployeeId(employeeId);
		brief.setOrderDate(LocalDate.now());
		BigDecimal total = BigDecimal.ZERO;
		for (OrderDetailVO detail : details) {
			total = total.add(detail.getAmount());
		}
		brief.setItems(new ArrayList<>(List.of(details)));
		brief.setTotalAmount(total);
		return brief;
	}

	private OrderDetailVO detail(Long recipeId, String name, String unit, int quantity) {
		OrderDetailVO detail = new OrderDetailVO();
		detail.setRecipeId(recipeId);
		detail.setRecipeName(name);
		detail.setCategory("热菜");
		detail.setUnit(unit);
		detail.setQuantity(quantity);
		detail.setAmount(new BigDecimal("10.00").multiply(BigDecimal.valueOf(quantity)));
		return detail;
	}

	private UserBriefVO brief(Long userId, String name, Long deptId, String workstation, String phone) {
		return new UserBriefVO(userId, "E" + userId, name, deptId, "研发部", workstation, phone);
	}

	private ServiceWindowVO window(LocalTime deliveryStart) {
		ServiceWindowVO vo = new ServiceWindowVO();
		vo.setCutoffTime(LocalTime.of(9, 0));
		vo.setDeliveryStartTime(deliveryStart);
		return vo;
	}
}
