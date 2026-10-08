package com.university.webdesign.service.operation;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.config.ServiceWindowProperties;
import com.university.webdesign.domain.operation.ServiceWindow;
import com.university.webdesign.domain.operation.WindowScope;
import com.university.webdesign.repository.operation.ServiceWindowRepository;
import com.university.webdesign.service.impl.operation.ServiceWindowServiceImpl;
import com.university.webdesign.service.operation.dto.ServiceWindowCmd;
import com.university.webdesign.service.operation.dto.ServiceWindowVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 时间窗口配置服务单元测试。
 * <p>
 * 用 Mockito 直接驱动 {@link ServiceWindowServiceImpl}，覆盖 M2 下单校验依赖的三条规则：
 * 无配置回退默认 09:00 / 11:30、{@code DEPT} 优先于 {@code GLOBAL}、
 * {@code canOrder} 按服务器时间与截止时间比较；以及配置新增/修改的校验与落库。
 */
class ServiceWindowServiceImplTest
{
	private final ServiceWindowRepository repository = mock(ServiceWindowRepository.class);

	private final ServiceWindowProperties properties = defaultProperties();

	private final ServiceWindowServiceImpl service =
			new ServiceWindowServiceImpl(repository, properties);

	private static ServiceWindowProperties defaultProperties() {
		return new ServiceWindowProperties();
	}

	@Test
	@DisplayName("数据库无配置时回退默认 09:00 / 11:30，并计算 canOrder")
	void fallsBackToDefaultWindow() {
		LocalDate date = LocalDate.now();
		when(repository.findEffective(eq(WindowScope.GLOBAL), eq(date))).thenReturn(List.of());

		ServiceWindowVO vo = service.get(date, null, null);

		assertThat(vo.getCutoffTime()).isEqualTo(LocalTime.of(9, 0));
		assertThat(vo.getDeliveryStartTime()).isEqualTo(LocalTime.of(11, 30));
		assertThat(vo.getConfigId()).isNull();
		assertThat(vo.getScope()).isEqualTo(WindowScope.GLOBAL.name());
		assertThat(vo.getServerTime()).isNotBlank();
		// 默认截止 09:00 已过（除非在 09:00 之前运行），这里只断言与本地时间一致
		boolean expected = LocalTime.now().isBefore(LocalTime.of(9, 0));
		assertThat(vo.isCanOrder()).isEqualTo(expected);
	}

	@Test
	@DisplayName("DEPT 配置优先于 GLOBAL 配置")
	void deptWindowWinsOverGlobal() {
		LocalDate date = LocalDate.now();
		ServiceWindow global = window(1L, WindowScope.GLOBAL, null, LocalTime.of(9, 0), LocalTime.of(11, 30));
		ServiceWindow dept = window(2L, WindowScope.DEPT, 7L, LocalTime.of(10, 15), LocalTime.of(12, 0));
		when(repository.findEffectiveByDept(WindowScope.DEPT, 7L, date)).thenReturn(List.of(dept));
		when(repository.findEffective(WindowScope.GLOBAL, date)).thenReturn(List.of(global));

		ServiceWindowVO vo = service.get(date, null, 7L);

		assertThat(vo.getConfigId()).isEqualTo(2L);
		assertThat(vo.getCutoffTime()).isEqualTo(LocalTime.of(10, 15));
		assertThat(vo.getDeliveryStartTime()).isEqualTo(LocalTime.of(12, 0));
		assertThat(vo.getScope()).isEqualTo(WindowScope.DEPT.name());
		assertThat(vo.getDeptId()).isEqualTo(7L);
	}

	@Test
	@DisplayName("部门无配置时退回全局配置")
	void fallsBackToGlobalWhenDeptMissing() {
		LocalDate date = LocalDate.now();
		ServiceWindow global = window(1L, WindowScope.GLOBAL, null, LocalTime.of(8, 30), LocalTime.of(11, 0));
		when(repository.findEffectiveByDept(WindowScope.DEPT, 9L, date)).thenReturn(List.of());
		when(repository.findEffective(WindowScope.GLOBAL, date)).thenReturn(List.of(global));

		ServiceWindowVO vo = service.get(date, null, 9L);

		assertThat(vo.getConfigId()).isEqualTo(1L);
		assertThat(vo.getCutoffTime()).isEqualTo(LocalTime.of(8, 30));
	}

	@Test
	@DisplayName("未来日期一定可以下单，历史日期一定不能下单")
	void canOrderDependsOnTargetDate() {
		ServiceWindowVO tomorrow = service.get(LocalDate.now().plusDays(1), null, null);
		assertThat(tomorrow.isCanOrder()).isTrue();

		ServiceWindow configured = window(1L, WindowScope.GLOBAL, null, LocalTime.of(23, 59), LocalTime.of(23, 59));
		when(repository.findEffective(eq(WindowScope.GLOBAL), any())).thenReturn(List.of(configured));
		ServiceWindowVO yesterday = service.get(LocalDate.now().minusDays(1), null, null);
		assertThat(yesterday.isCanOrder()).isFalse();
	}

	@Test
	@DisplayName("新增配置：截止时间必须早于配餐开始时间")
	void createRejectsInvalidTimes() {
		ServiceWindowCmd cmd = new ServiceWindowCmd();
		cmd.setCutoffTime(LocalTime.of(12, 0));
		cmd.setDeliveryStartTime(LocalTime.of(11, 0));

		assertThatThrownBy(() -> service.create(cmd))
				.isInstanceOf(BusinessException.class)
				.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
						.isEqualTo(ErrorCode.PARAM_INVALID));
		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("新增配置：scope=DEPT 时部门ID必填")
	void createRequiresDeptForDeptScope() {
		ServiceWindowCmd cmd = new ServiceWindowCmd();
		cmd.setCutoffTime(LocalTime.of(9, 0));
		cmd.setDeliveryStartTime(LocalTime.of(11, 30));
		cmd.setScope("DEPT");

		assertThatThrownBy(() -> service.create(cmd))
				.isInstanceOf(BusinessException.class)
				.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
						.isEqualTo(ErrorCode.PARAM_INVALID));
	}

	@Test
	@DisplayName("新增配置：同作用域同生效日期重复时抛 40901")
	void createRejectsDuplicatedEffectiveDate() {
		ServiceWindowCmd cmd = new ServiceWindowCmd();
		cmd.setCutoffTime(LocalTime.of(9, 0));
		cmd.setDeliveryStartTime(LocalTime.of(11, 30));
		cmd.setEffectiveFrom(LocalDate.of(2026, 1, 1));
		when(repository.existsByScopeAndDeptIdAndEffectiveFrom(WindowScope.GLOBAL, null, LocalDate.of(2026, 1, 1)))
				.thenReturn(true);

		assertThatThrownBy(() -> service.create(cmd))
				.isInstanceOf(BusinessException.class)
				.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
						.isEqualTo(ErrorCode.DUPLICATED));
	}

	@Test
	@DisplayName("新增配置成功时落库并返回配置ID")
	void createPersistsEntity() {
		ServiceWindowCmd cmd = new ServiceWindowCmd();
		cmd.setCutoffTime(LocalTime.of(9, 30));
		cmd.setDeliveryStartTime(LocalTime.of(11, 45));
		cmd.setEffectiveFrom(LocalDate.of(2026, 3, 1));
		when(repository.save(any(ServiceWindow.class))).thenAnswer(invocation -> {
			ServiceWindow entity = invocation.getArgument(0);
			entity.setId(66L);
			return entity;
		});

		Long id = service.create(cmd);

		assertThat(id).isEqualTo(66L);
		verify(repository).save(any(ServiceWindow.class));
	}

	@Test
	@DisplayName("修改不存在的配置抛 40400")
	void updateMissingConfig() {
		ServiceWindowCmd cmd = new ServiceWindowCmd();
		cmd.setCutoffTime(LocalTime.of(9, 0));
		cmd.setDeliveryStartTime(LocalTime.of(11, 30));
		when(repository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(404L, cmd))
				.isInstanceOf(BusinessException.class)
				.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
						.isEqualTo(ErrorCode.NOT_FOUND));
	}

	@Test
	@DisplayName("修改配置后立即生效（写回时间字段）")
	void updateOverwritesTimes() {
		ServiceWindow entity = window(5L, WindowScope.GLOBAL, null, LocalTime.of(9, 0), LocalTime.of(11, 30));
		when(repository.findById(5L)).thenReturn(Optional.of(entity));
		ServiceWindowCmd cmd = new ServiceWindowCmd();
		cmd.setCutoffTime(LocalTime.of(8, 45));
		cmd.setDeliveryStartTime(LocalTime.of(11, 15));

		service.update(5L, cmd);

		assertThat(entity.getCutoffTime()).isEqualTo(LocalTime.of(8, 45));
		assertThat(entity.getDeliveryStartTime()).isEqualTo(LocalTime.of(11, 15));
		verify(repository).save(entity);
	}

	private ServiceWindow window(Long id, WindowScope scope, Long deptId, LocalTime cutoff, LocalTime deliveryStart) {
		ServiceWindow entity = new ServiceWindow();
		entity.setId(id);
		entity.setScope(scope);
		entity.setDeptId(deptId);
		entity.setCutoffTime(cutoff);
		entity.setDeliveryStartTime(deliveryStart);
		entity.setEffectiveFrom(LocalDate.of(2026, 1, 1));
		return entity;
	}
}
