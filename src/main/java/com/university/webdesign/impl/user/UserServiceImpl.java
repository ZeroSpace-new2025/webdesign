package com.university.webdesign.impl.user;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import com.university.webdesign.common.PageResult;
import com.university.webdesign.common.TabularExport;
import com.university.webdesign.domain.user.Role;
import com.university.webdesign.domain.user.User;
import com.university.webdesign.domain.user.UserStatus;
import com.university.webdesign.event.RolePermissionChangedEvent;
import com.university.webdesign.event.UserUpdatedEvent;
import com.university.webdesign.repository.user.RoleRepository;
import com.university.webdesign.repository.user.UserRepository;
import com.university.webdesign.service.user.UserService;
import com.university.webdesign.service.user.dto.ImportResultVO;
import com.university.webdesign.service.user.dto.RoleVO;
import com.university.webdesign.service.user.dto.UserBriefVO;
import com.university.webdesign.service.user.dto.UserCreateCmd;
import com.university.webdesign.service.user.dto.UserQuery;
import com.university.webdesign.service.user.dto.UserUpdateCmd;
import com.university.webdesign.service.user.dto.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 用户（员工）维护服务实现。
 * <p>
 * 对应《对外方法表》5.2 的 {@code UserService} 全部方法；
 * 身份不再由前端传入 {@code operatorId}，需要管理员权限的写操作通过
 * {@link com.university.webdesign.common.UserContextHolder#require()} 取当前登录用户判定。
 * <p>
 * 事务统一用普通 {@code @Transactional}（含查询），**不加**
 * {@code readOnly = true}——项目历史上只读事务会把 Hibernate flush 模式切成 MANUAL，
 * 导致同一事务内“先改后查”读到旧数据。
 * <p>
 * 模块自足：本类只依赖 {@code common}、本模块的实体/仓库/DTO 与事件，
 * 不依赖 M2/M3 的任何类型（避免环形依赖）。
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService
{
	/**
	 * 默认初始密码
	 */
	private static final String DEFAULT_INITIAL_PASSWORD = "123456";

	/**
	 * 密码最短长度
	 */
	private static final int MIN_PASSWORD_LENGTH = 6;

	/**
	 * 手机号校验：允许 11 位手机号或带区号的座机
	 */
	private static final String PHONE_PATTERN = "^(1\\d{10}|0\\d{2,3}-?\\d{7,8})$";

	/**
	 * 导入模板表头（中英文二选一，解析时两种写法都能识别）
	 */
	private static final List<String> TEMPLATE_HEADER =
			List.of("工号(employeeNo)", "姓名(name)", "部门ID(deptId)", "部门名称(deptName)",
					"工位(workstation)", "电话(phone)");

	/**
	 * 表头关键字 → 逻辑列名
	 */
	private static final Map<String, String> HEADER_ALIASES = buildHeaderAliases();

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 构造器注入
	 *
	 * @param userRepository  用户仓库
	 * @param roleRepository  角色仓库
	 * @param passwordEncoder BCrypt 密码编码器
	 * @param eventPublisher  领域事件发布器
	 */
	public UserServiceImpl(
			UserRepository userRepository,
			RoleRepository roleRepository,
			PasswordEncoder passwordEncoder,
			ApplicationEventPublisher eventPublisher) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.eventPublisher = eventPublisher;
	}

	@Override
	@Transactional
	public Long create(UserCreateCmd cmd) {
		if (cmd == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "创建入参不能为空");
		}
		String employeeNo = required(cmd.getEmployeeNo(), "工号不能为空");
		if (userRepository.existsByEmployeeNoIgnoreCase(employeeNo)) {
			throw new BusinessException(ErrorCode.DUPLICATED, "工号已存在：" + employeeNo);
		}
		String phone = trimToNull(cmd.getPhone());
		if (phone != null) {
			validatePhone(phone);
			if (userRepository.existsByPhone(phone)) {
				throw new BusinessException(ErrorCode.DUPLICATED, "手机号已被占用：" + phone);
			}
		}
		User user = new User();
		user.setEmployeeNo(employeeNo);
		user.setName(required(cmd.getName(), "姓名不能为空"));
		applyDepartment(user, cmd.getDeptId(), cmd.getDeptName());
		user.setWorkstation(trimToNull(cmd.getWorkstation()));
		user.setPhone(phone);
		user.setStatus(UserStatus.ACTIVE);
		user.setPassword(passwordEncoder.encode(passwordOrDefault(cmd.getInitialPassword())));
		user.setRoles(new LinkedHashSet<>(resolveRoles(cmd.getRoleIds())));
		User saved = userRepository.save(user);
		log.info("创建员工账号：工号={}，用户ID={}", saved.getEmployeeNo(), saved.getId());
		publishUpdated(saved);
		return saved.getId();
	}

	@Override
	@Transactional
	public PageResult<UserVO> page(UserQuery q) {
		UserQuery query = q == null ? new UserQuery() : q;
		UserStatus status = parseStatus(query.getStatus());
		Pageable pageable = PageRequest.of(query.toSpringPageNumber(), query.normalizedPageSize());
		Page<User> page = userRepository.search(
				trimToNull(query.getKeyword()), query.getDeptId(),
				trimToNull(query.getWorkstation()), status, pageable);
		return PageResult.from(page, this::toVO);
	}

	@Override
	@Transactional
	public UserVO getById(Long userId) {
		User user = loadWithRoles(userId);
		return toVO(user);
	}

	@Override
	@Transactional
	public void update(Long userId, UserUpdateCmd cmd) {
		if (cmd == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "更新入参不能为空");
		}
		User user = loadWithRoles(userId);
		if (cmd.getName() != null) {
			user.setName(required(cmd.getName(), "姓名不能为空"));
		}
		if (cmd.getDeptId() != null || cmd.getDeptName() != null) {
			applyDepartment(user, cmd.getDeptId(), cmd.getDeptName());
		}
		if (cmd.getWorkstation() != null) {
			user.setWorkstation(trimToNull(cmd.getWorkstation()));
		}
		if (cmd.getPhone() != null) {
			String phone = trimToNull(cmd.getPhone());
			if (phone != null && !phone.equals(user.getPhone())) {
				validatePhone(phone);
				if (userRepository.existsByPhone(phone)) {
					throw new BusinessException(ErrorCode.DUPLICATED, "手机号已被占用：" + phone);
				}
			}
			user.setPhone(phone);
		}
		User saved = userRepository.save(user);
		log.info("更新员工信息：用户ID={}，工位={}", saved.getId(), saved.getWorkstation());
		publishUpdated(saved);
	}

	@Override
	@Transactional
	public void disable(Long userId) {
		User user = loadWithRoles(userId);
		user.setStatus(UserStatus.DISABLED);
		User saved = userRepository.save(user);
		log.info("停用员工账号：用户ID={}", saved.getId());
		publishUpdated(saved);
	}

	@Override
	@Transactional
	public ImportResultVO importFromExcel(MultipartFile file, Long deptId, List<Long> roleIds) {
		List<List<String>> rows = UserImportParser.readRows(file);
		ImportResultVO result = new ImportResultVO();
		if (rows.isEmpty()) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "导入文件内容为空");
		}
		Map<String, Integer> header = parseHeader(rows.get(0));
		if (!header.containsKey("employeeNo") || !header.containsKey("name")) {
			throw new BusinessException(ErrorCode.PARAM_INVALID,
					"模板表头不正确，至少需要“工号”和“姓名”两列");
		}
		Set<String> seenEmployeeNos = new LinkedHashSet<>();
		for (int index = 1; index < rows.size(); index++) {
			List<String> row = rows.get(index);
			int rowNo = index + 1;
			result.setTotal(result.getTotal() + 1);
			try {
				UserCreateCmd cmd = toCreateCmd(row, header, deptId, roleIds, seenEmployeeNos);
				create(cmd);
				result.setSuccessCount(result.getSuccessCount() + 1);
			} catch (BusinessException exception) {
				result.addError(rowNo, exception.getMessage());
			} catch (RuntimeException exception) {
				log.warn("导入第 {} 行失败", rowNo, exception);
				result.addError(rowNo, "导入失败：" + exception.getMessage());
			}
		}
		log.info("员工批量导入完成：总计 {} 行，成功 {} 行，失败 {} 行",
				result.getTotal(), result.getSuccessCount(), result.getFailCount());
		return result;
	}

	@Override
	@Transactional
	public Resource exportImportTemplate() {
		// 离线环境不引入 Excel 生成库，模板用带 UTF-8 BOM 的 CSV（Excel 可直接打开）
		return TabularExport.csv(TEMPLATE_HEADER,
				List.of(
						List.of("E1001", "张三", "10", "研发部", "R-12", "13800000001"),
						List.of("E1002", "李四", "20", "产品部", "P-08", "13800000002")));
	}

	@Override
	@Transactional
	public void changeStatus(Long userId, UserStatus status) {
		if (status == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID,
					"账号状态不能为空，可选值：ACTIVE / DISABLED / LOCKED");
		}
		User user = loadWithRoles(userId);
		user.setStatus(status);
		User saved = userRepository.save(user);
		log.info("变更员工账号状态：用户ID={}，状态={}", saved.getId(), status);
		publishUpdated(saved);
	}

	@Override
	@Transactional
	public void assignRoles(Long userId, List<Long> roleIds) {
		User user = loadWithRoles(userId);
		Set<Role> roles = new LinkedHashSet<>(resolveRoles(roleIds));
		user.setRoles(roles);
		User saved = userRepository.save(user);
		log.info("分配员工角色：用户ID={}，角色数={}", saved.getId(), roles.size());
		publishUpdated(saved);
		eventPublisher.publishEvent(new RolePermissionChangedEvent(null, saved.getId(),
				List.copyOf(permCodesOf(saved))));
	}

	@Override
	@Transactional
	public List<UserBriefVO> listByIds(Collection<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return List.of();
		}
		Set<Long> ids = new LinkedHashSet<>();
		for (Long id : userIds) {
			if (id != null) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return List.of();
		}
		return userRepository.findAllByIdIn(ids).stream().map(this::toBrief).toList();
	}

	@Override
	@Transactional
	public UserBriefVO getBrief(Long userId) {
		if (userId == null) {
			return null;
		}
		return userRepository.findById(userId).map(this::toBrief).orElse(null);
	}

	@Override
	@Transactional
	public boolean hasAnyRole(Long userId, String... roleCodes) {
		if (userId == null || roleCodes == null || roleCodes.length == 0) {
			return false;
		}
		Set<String> owned = userRepository.findWithRolesById(userId)
				.map(User::roleNames)
				.orElseGet(LinkedHashSet::new);
		if (owned.isEmpty()) {
			return false;
		}
		for (String expected : roleCodes) {
			if (expected == null) {
				continue;
			}
			for (String actual : owned) {
				if (expected.equalsIgnoreCase(actual)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 按ID读取用户并抓取角色，不存在抛 40400
	 *
	 * @param userId 用户ID
	 * @return 用户实体
	 */
	private User loadWithRoles(Long userId) {
		if (userId == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "用户ID不能为空");
		}
		return userRepository.findWithRolesById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "员工不存在：" + userId));
	}

	/**
	 * 解析角色ID集合为角色实体，任一不存在抛 40400
	 *
	 * @param roleIds 角色ID集合
	 * @return 角色实体集合
	 */
	private List<Role> resolveRoles(List<Long> roleIds) {
		if (roleIds == null || roleIds.isEmpty()) {
			return List.of();
		}
		Set<Long> ids = new LinkedHashSet<>();
		for (Long id : roleIds) {
			if (id != null) {
				ids.add(id);
			}
		}
		List<Role> roles = roleRepository.findAllByIdIn(ids);
		if (roles.size() != ids.size()) {
			Set<Long> found = new LinkedHashSet<>();
			roles.forEach(role -> found.add(role.getId()));
			ids.removeAll(found);
			throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在：" + ids);
		}
		return roles;
	}

	/**
	 * 展开用户拥有的权限点编码
	 *
	 * @param user 用户实体
	 * @return 权限点编码集合
	 */
	private Set<String> permCodesOf(User user) {
		Set<String> codes = new LinkedHashSet<>();
		for (Role role : user.getRoles()) {
			if (role != null) {
				codes.addAll(role.permCodes());
			}
		}
		return codes;
	}

	/**
	 * 发布员工信息变更事件（M3 配送信息同步、本模块鉴权缓存失效）
	 *
	 * @param user 用户实体
	 */
	private void publishUpdated(User user) {
		eventPublisher.publishEvent(new UserUpdatedEvent(user.getId(), user.getDeptId(),
				user.getWorkstation(), user.getPhone()));
	}

	/**
	 * 员工实体 → 用户视图
	 *
	 * @param user 用户实体
	 * @return 用户视图
	 */
	private UserVO toVO(User user) {
		UserVO vo = new UserVO();
		vo.setUserId(user.getId());
		vo.setEmployeeNo(user.getEmployeeNo());
		vo.setName(user.getName());
		vo.setDeptId(user.getDeptId());
		vo.setDeptName(user.getDeptName());
		vo.setWorkstation(user.getWorkstation());
		vo.setPhone(user.getPhone());
		UserStatus status = user.getStatus() == null ? UserStatus.ACTIVE : user.getStatus();
		vo.setStatus(status.name());
		vo.setStatusText(status.getText());
		List<RoleVO> roles = new ArrayList<>();
		List<Long> roleIds = new ArrayList<>();
		List<String> roleCodes = new ArrayList<>();
		for (Role role : user.getRoles()) {
			if (role == null) {
				continue;
			}
			RoleVO roleVO = new RoleVO();
			roleVO.setRoleId(role.getId());
			roleVO.setName(role.getName());
			roleVO.setPermCodes(new LinkedHashSet<>(role.permCodes()));
			roles.add(roleVO);
			roleIds.add(role.getId());
			roleCodes.add(role.getName());
		}
		vo.setRoles(roles);
		vo.setRoleIds(roleIds);
		vo.setRoleCodes(roleCodes);
		return vo;
	}

	/**
	 * 员工实体 → 精简视图
	 *
	 * @param user 用户实体
	 * @return 精简视图
	 */
	private UserBriefVO toBrief(User user) {
		return new UserBriefVO(user.getId(), user.getEmployeeNo(), user.getName(), user.getDeptId(),
				user.getDeptName(), user.getWorkstation(), user.getPhone());
	}

	/**
	 * 解析状态编码，非法编码抛 40001
	 *
	 * @param code 状态编码
	 * @return 状态；入参为空返回 null
	 */
	private UserStatus parseStatus(String code) {
		if (code == null || code.isBlank()) {
			return null;
		}
		UserStatus status = UserStatus.parse(code);
		if (status == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID,
					"账号状态不合法：" + code + "（可选 ACTIVE / DISABLED / LOCKED）");
		}
		return status;
	}

	/**
	 * 校验并写入部门信息
	 *
	 * @param user     用户实体
	 * @param deptId   部门ID
	 * @param deptName 部门名称
	 */
	private void applyDepartment(User user, Long deptId, String deptName) {
		user.setDeptId(deptId);
		user.setDeptName(trimToNull(deptName));
	}

	/**
	 * 校验手机号格式
	 *
	 * @param phone 手机号
	 */
	private void validatePhone(String phone) {
		if (!phone.matches(PHONE_PATTERN)) {
			throw new BusinessException(ErrorCode.PARAM_INVALID,
					"联系电话格式不正确：" + phone + "（11 位手机号或带区号座机）");
		}
	}

	/**
	 * 起始密码：为空时使用默认初始密码
	 *
	 * @param initialPassword 入参密码
	 * @return 实际使用的明文密码
	 */
	private String passwordOrDefault(String initialPassword) {
		String password = trimToNull(initialPassword);
		if (password == null) {
			return DEFAULT_INITIAL_PASSWORD;
		}
		if (password.length() < MIN_PASSWORD_LENGTH) {
			throw new BusinessException(ErrorCode.PARAM_INVALID,
					"初始密码长度不能少于 " + MIN_PASSWORD_LENGTH + " 位");
		}
		return password;
	}

	/**
	 * 必填校验
	 *
	 * @param value   原值
	 * @param message 为空时的提示
	 * @return 去空白后的值
	 */
	private String required(String value, String message) {
		String normalized = trimToNull(value);
		if (normalized == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, message);
		}
		return normalized;
	}

	/**
	 * 去空白，空白返回 null
	 *
	 * @param value 原值
	 * @return 归一化后的值
	 */
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}

	/**
	 * 解析导入表头，支持中英文两种写法
	 *
	 * @param headerRow 表头行
	 * @return 逻辑列名 → 列下标
	 */
	private Map<String, Integer> parseHeader(List<String> headerRow) {
		Map<String, Integer> header = new LinkedHashMap<>();
		for (int index = 0; index < headerRow.size(); index++) {
			String cell = headerRow.get(index);
			if (cell == null || cell.isBlank()) {
				continue;
			}
			String normalized = cell.replace(" ", "").toLowerCase(Locale.ROOT);
			for (Map.Entry<String, String> alias : HEADER_ALIASES.entrySet()) {
				if (normalized.contains(alias.getKey())) {
					header.putIfAbsent(alias.getValue(), index);
					break;
				}
			}
		}
		return header;
	}

	/**
	 * 把一行单元格转成创建入参，并校验工号在文件内不重复
	 *
	 * @param row             单元格列表
	 * @param header          表头映射
	 * @param defaultDeptId   统一部门ID
	 * @param defaultRoleIds  统一角色ID
	 * @param seenEmployeeNos 文件内已出现的工号
	 * @return 创建入参
	 */
	private UserCreateCmd toCreateCmd(List<String> row, Map<String, Integer> header,
			Long defaultDeptId, List<Long> defaultRoleIds, Set<String> seenEmployeeNos) {
		UserCreateCmd cmd = new UserCreateCmd();
		String employeeNo = cell(row, header, "employeeNo");
		if (employeeNo == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "工号不能为空");
		}
		if (!seenEmployeeNos.add(employeeNo.toLowerCase(Locale.ROOT))) {
			throw new BusinessException(ErrorCode.DUPLICATED, "工号在文件中重复：" + employeeNo);
		}
		String name = cell(row, header, "name");
		if (name == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "姓名不能为空");
		}
		cmd.setEmployeeNo(employeeNo);
		cmd.setName(name);
		String deptIdText = cell(row, header, "deptId");
		Long deptId = defaultDeptId;
		if (deptIdText != null) {
			try {
				deptId = Long.valueOf(deptIdText);
			} catch (NumberFormatException exception) {
				throw new BusinessException(ErrorCode.PARAM_INVALID, "部门ID必须是数字：" + deptIdText);
			}
		}
		cmd.setDeptId(deptId);
		cmd.setDeptName(cell(row, header, "deptName"));
		cmd.setWorkstation(cell(row, header, "workstation"));
		cmd.setPhone(cell(row, header, "phone"));
		cmd.setRoleIds(defaultRoleIds);
		return cmd;
	}

	/**
	 * 取单元格文本，越界或空白返回 null
	 *
	 * @param row    单元格列表
	 * @param header 表头映射
	 * @param column 逻辑列名
	 * @return 文本
	 */
	private String cell(List<String> row, Map<String, Integer> header, String column) {
		Integer index = header.get(column);
		if (index == null || index < 0 || index >= row.size()) {
			return null;
		}
		return trimToNull(row.get(index));
	}

	/**
	 * 表头别名表：同时接受中英文与常见变体
	 *
	 * @return 别名 → 逻辑列名
	 */
	private static Map<String, String> buildHeaderAliases() {
		Map<String, String> aliases = new LinkedHashMap<>();
		aliases.put("employeeno", "employeeNo");
		aliases.put("工号", "employeeNo");
		aliases.put("员工编号", "employeeNo");
		aliases.put("name", "name");
		aliases.put("姓名", "name");
		aliases.put("deptid", "deptId");
		aliases.put("部门id", "deptId");
		aliases.put("部门编号", "deptId");
		aliases.put("deptname", "deptName");
		aliases.put("部门名称", "deptName");
		aliases.put("部门", "deptName");
		aliases.put("workstation", "workstation");
		aliases.put("工位", "workstation");
		aliases.put("phone", "phone");
		aliases.put("电话", "phone");
		aliases.put("联系电话", "phone");
		aliases.put("手机号", "phone");
		return aliases;
	}
}
