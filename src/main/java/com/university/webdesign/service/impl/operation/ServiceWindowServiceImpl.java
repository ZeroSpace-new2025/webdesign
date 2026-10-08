package com.university.webdesign.service.impl.operation;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.config.ServiceWindowProperties;
import com.university.webdesign.domain.operation.ServiceWindow;
import com.university.webdesign.domain.operation.WindowScope;
import com.university.webdesign.repository.operation.ServiceWindowRepository;
import com.university.webdesign.service.operation.ServiceWindowService;
import com.university.webdesign.service.operation.dto.ServiceWindowCmd;
import com.university.webdesign.service.operation.dto.ServiceWindowVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 时间窗口配置服务实现。
 * <p>
 * 对应《对外方法表》4.2 的 {@code ServiceWindowService}：
 * <ul>
 *     <li>{@link #get(LocalDate, String, Long)} 是 M2 下单校验与 M3 配送放行的
 *         <b>唯一时间窗口数据来源</b>：{@code DEPT} 优先于 {@code GLOBAL}，
 *         取 {@code effective_from <= date}（为空视为立即生效）中最新的一条；
 *         查不到则回退 {@link ServiceWindowProperties} 的默认值（09:00 / 11:30）；</li>
 *     <li>{@link #create}/{@link #update} 落库到 {@code service_window}，改后立即生效
 *         （每次查询都重新读库，无需缓存失效）。</li>
 * </ul>
 * 事务统一用普通 {@code @Transactional}，查询也不加 {@code readOnly = true}。
 */
@Service
@Transactional
public class ServiceWindowServiceImpl implements ServiceWindowService
{
	/**
	 * 服务器时间文案格式
	 */
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final ServiceWindowRepository serviceWindowRepository;
	private final ServiceWindowProperties properties;

	/**
	 * 构造器注入（唯一的公开构造器，Spring 自动装配）
	 * <p>
	 * 直接注入 {@link ServiceWindowProperties}：该类已标注 {@code @Component} 并被注册为 Bean。
	 * 曾经的“两个公开构造器 + ObjectProvider 可选注入”写法会让 Spring 无法确定用哪一个，
	 * 直接以 {@code No default constructor found} 让整个上下文启动失败——这是本次重构中
	 * 由集成测试抓出来的真实缺陷。
	 *
	 * @param serviceWindowRepository 时间窗口仓储
	 * @param properties              兜底配置（数据库无配置时使用 09:00 / 11:30）
	 */
	public ServiceWindowServiceImpl(ServiceWindowRepository serviceWindowRepository,
			ServiceWindowProperties properties) {
		this.serviceWindowRepository = serviceWindowRepository;
		this.properties = properties;
	}

	@Override
	public Long create(ServiceWindowCmd cmd) {
		ServiceWindowCmd request = validate(cmd);
		WindowScope scope = WindowScope.parse(request.getScope());
		Long deptId = scope == WindowScope.DEPT ? request.getDeptId() : null;
		LocalDate effectiveFrom = request.getEffectiveFrom();

		// 重叠校验：同一作用域（同部门）同一生效日期只允许一条配置，避免取值歧义
		if (serviceWindowRepository.existsByScopeAndDeptIdAndEffectiveFrom(scope, deptId, effectiveFrom)) {
			throw new BusinessException(ErrorCode.DUPLICATED,
					"该作用域在 " + (effectiveFrom == null ? "立即生效" : effectiveFrom)
							+ " 已存在时间窗口配置，请改为修改既有配置");
		}

		ServiceWindow entity = new ServiceWindow();
		entity.setCutoffTime(request.getCutoffTime());
		entity.setDeliveryStartTime(request.getDeliveryStartTime());
		entity.setEffectiveFrom(effectiveFrom);
		entity.setScope(scope);
		entity.setDeptId(deptId);
		return serviceWindowRepository.save(entity).getId();
	}

	@Override
	public void update(Long configId, ServiceWindowCmd cmd) {
		if (configId == null) {
			throw BusinessException.paramInvalid("配置ID不能为空");
		}
		ServiceWindowCmd request = validate(cmd);
		ServiceWindow entity = serviceWindowRepository.findById(configId)
				.orElseThrow(() -> BusinessException.notFound("时间窗口配置不存在：" + configId));

		// 修改后立即生效：M2 下单校验每次都会重新读取，无需额外刷新缓存
		entity.setCutoffTime(request.getCutoffTime());
		entity.setDeliveryStartTime(request.getDeliveryStartTime());
		entity.setEffectiveFrom(request.getEffectiveFrom());
		if (request.getScope() != null && !request.getScope().isBlank()) {
			WindowScope scope = WindowScope.parse(request.getScope());
			entity.setScope(scope);
			entity.setDeptId(scope == WindowScope.DEPT ? request.getDeptId() : null);
		}
		serviceWindowRepository.save(entity);
	}

	@Override
	public ServiceWindowVO get(LocalDate date, String scope, Long deptId) {
		LocalDate target = date == null ? LocalDate.now() : date;
		// scope 参数用于显式指定作用域；当前取值规则固定为“部门优先于全局”，
		// 因此只在指定 GLOBAL 时跳过部门配置，其余情况等价于自动选择。
		Long effectiveDeptId = WindowScope.GLOBAL.name().equalsIgnoreCase(scope == null ? "" : scope.trim())
				? null : deptId;
		ServiceWindow effective = resolveConfig(target, effectiveDeptId);

		ServiceWindowVO vo = new ServiceWindowVO();
		vo.setConfigId(effective == null ? null : effective.getId());
		vo.setCutoffTime(effective == null || effective.getCutoffTime() == null
				? resolveProperties().getDefaultCutoffTime() : effective.getCutoffTime());
		vo.setDeliveryStartTime(effective == null || effective.getDeliveryStartTime() == null
				? resolveProperties().getDefaultDeliveryStartTime() : effective.getDeliveryStartTime());
		vo.setEffectiveFrom(effective == null ? null : effective.getEffectiveFrom());
		vo.setScope(effective == null ? WindowScope.GLOBAL.name() : effective.getScope().name());
		vo.setDeptId(effective == null ? null : effective.getDeptId());
		vo.setServerTime(DATE_TIME.format(LocalDateTime.now()));
		vo.setCanOrder(canOrder(target, vo.getCutoffTime()));
		// 当日订单数属于订单模块的数据，跨模块取数未在本模块的契约依赖表内登记，
		// 因此这里保持 0，仅供页面提示用，不作为业务判定依据。
		vo.setTodayOrderCount(0);
		return vo;
	}

	/**
	 * 解析生效的时间窗口配置
	 * <p>
	 * 取值优先级：{@code DEPT}（指定部门）> {@code GLOBAL}；
	 * 同作用域内仓储已按 {@code effective_from} 倒序（NULL 在前）与主键倒序返回，取第一条即可。
	 *
	 * @param date   目标日期
	 * @param deptId 部门ID，可为 null
	 * @return 生效配置；无可用配置时返回 null
	 */
	private ServiceWindow resolveConfig(LocalDate date, Long deptId) {
		if (deptId != null) {
			List<ServiceWindow> deptConfigs =
					serviceWindowRepository.findEffectiveByDept(WindowScope.DEPT, deptId, date);
			if (!deptConfigs.isEmpty()) {
				return deptConfigs.get(0);
			}
		}
		List<ServiceWindow> globalConfigs =
				serviceWindowRepository.findEffective(WindowScope.GLOBAL, date);
		return globalConfigs.isEmpty() ? null : globalConfigs.get(0);
	}

	/**
	 * 判断目标日期当前是否仍可下单
	 * <p>
	 * 规则：历史日期一定不可下单（不能给昨天补单）；未来日期可以提前预订（需求未禁止，
	 * 由业务方按需收紧）；当天则比较当前时间是否早于订餐截止时间。
	 *
	 * @param target 目标就餐日期
	 * @param cutoff 生效的订餐截止时间
	 * @return 可下单返回 true
	 */
	private boolean canOrder(LocalDate target, LocalTime cutoff) {
		LocalDate today = LocalDate.now();
		if (target.isBefore(today)) {
			return false;
		}
		if (target.isAfter(today)) {
			return true;
		}
		return cutoff == null || LocalTime.now().isBefore(cutoff);
	}

	/**
	 * 取兜底配置
	 *
	 * @return 兜底配置（永不为 null）
	 */
	private ServiceWindowProperties resolveProperties() {
		return properties == null ? new ServiceWindowProperties() : properties;
	}

	/**
	 * 校验入参
	 *
	 * @param cmd 配置入参
	 * @return 非空入参
	 */
	private ServiceWindowCmd validate(ServiceWindowCmd cmd) {
		if (cmd == null) {
			throw BusinessException.paramInvalid("时间窗口配置不能为空");
		}
		if (cmd.getCutoffTime() == null) {
			throw BusinessException.paramInvalid("订餐截止时间不能为空");
		}
		if (cmd.getDeliveryStartTime() == null) {
			throw BusinessException.paramInvalid("配餐开始时间不能为空");
		}
		if (!cmd.getCutoffTime().isBefore(cmd.getDeliveryStartTime())) {
			throw BusinessException.paramInvalid("订餐截止时间必须早于配餐开始时间");
		}
		WindowScope scope = WindowScope.parse(cmd.getScope());
		if (scope == WindowScope.DEPT && cmd.getDeptId() == null) {
			throw BusinessException.paramInvalid("按部门配置时间窗口时部门ID不能为空");
		}
		return cmd;
	}
}
