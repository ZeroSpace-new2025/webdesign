package com.university.webdesign.service.user;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.service.user.dto.ImportResultVO;
import com.university.webdesign.service.user.dto.UserBriefVO;
import com.university.webdesign.service.user.dto.UserCreateCmd;
import com.university.webdesign.service.user.dto.UserQuery;
import com.university.webdesign.service.user.dto.UserUpdateCmd;
import com.university.webdesign.service.user.dto.UserVO;
import com.university.webdesign.domain.user.UserStatus;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

/**
 * 用户（员工）维护服务。
 * <p>
 * 对应《对外方法表》5.2 的 {@code UserService}：员工增删查改、批量导入、角色分配，
 * 以及供其他模块使用的批量查询契约（{@link #listByIds} / {@link #getBrief} /
 * {@link #hasAnyRole}）。
 * <p>
 * 身份传递：管理员类操作不接收 {@code operatorId}，由实现类通过
 * {@link com.university.webdesign.common.UserContextHolder#require()} 取当前登录用户后校验角色。
 */
public interface UserService
{
	/**
	 * 创建员工账号（M4-05）
	 * <p>
	 * 校验工号与手机号唯一（冲突 40901），密码用 BCrypt 加密，
	 * {@code cmd.initialPassword} 为空时使用默认初始密码 {@code 123456}，并绑定角色。
	 *
	 * @param cmd 创建入参
	 * @return 新用户ID
	 */
	Long create(UserCreateCmd cmd);

	/**
	 * 分页查询用户（M4-06）
	 *
	 * @param q 查询条件
	 * @return 分页用户列表
	 */
	PageResult<UserVO> page(UserQuery q);

	/**
	 * 查询用户详情（M4-07），含已分配角色
	 *
	 * @param userId 用户ID
	 * @return 用户视图
	 */
	UserVO getById(Long userId);

	/**
	 * 更新用户信息（M4-08）
	 * <p>
	 * 修改姓名、部门、工位、电话；工位变更影响后续配送单，因此发布
	 * {@link com.university.webdesign.event.UserUpdatedEvent}。
	 *
	 * @param userId 用户ID
	 * @param cmd    更新入参
	 */
	void update(Long userId, UserUpdateCmd cmd);

	/**
	 * 逻辑停用用户（M4-09）
	 * <p>
	 * 置为 {@link UserStatus#DISABLED}，不删除历史订单归属。
	 *
	 * @param userId 用户ID
	 */
	void disable(Long userId);

	/**
	 * 批量导入员工（M4-10）
	 * <p>
	 * 逐行校验（工号非空且未重复、姓名非空、电话格式），支持部分成功；
	 * 单元格解析沿用项目内的轻量 CSV/文本解析（不引入 EasyExcel / POI），
	 * 表头支持中英文两种写法。
	 *
	 * @param file   上传文件（.csv / .txt / .xlsx）
	 * @param deptId 统一指定的部门ID，可为空（为空时该列必须填写）
	 * @param roleIds 统一分配的角色ID集合，可为空
	 * @return 导入结果（含逐行错误）
	 */
	ImportResultVO importFromExcel(MultipartFile file, Long deptId, List<Long> roleIds);

	/**
	 * 下载导入模板（M4-11）
	 * <p>
	 * 当前返回 UTF-8 BOM 的 CSV 模板（离线环境不引入 Excel 生成库），
	 * 文件名由 Controller 通过 {@code Content-Disposition} 指定。
	 *
	 * @return 模板文件资源
	 */
	Resource exportImportTemplate();

	/**
	 * 更新账号状态（M4-12）
	 *
	 * @param userId 用户ID
	 * @param status 目标状态 ACTIVE / DISABLED / LOCKED
	 */
	void changeStatus(Long userId, UserStatus status);

	/**
	 * 分配用户角色（M4-20），全量覆盖
	 * <p>
	 * 重建用户-角色关系并发布
	 * {@link com.university.webdesign.event.RolePermissionChangedEvent} 使鉴权缓存失效。
	 *
	 * @param userId  用户ID
	 * @param roleIds 角色ID集合，为空表示清空角色
	 */
	void assignRoles(Long userId, List<Long> roleIds);

	/**
	 * 批量查询员工精简信息（跨模块契约）
	 * <p>
	 * 供 M2 订单列表补齐姓名/部门、M3 配送单补齐工位/电话使用；批量查询避免 N+1。
	 *
	 * @param userIds 用户ID集合
	 * @return 员工精简信息列表，入参为空时返回空列表
	 */
	List<UserBriefVO> listByIds(Collection<Long> userIds);

	/**
	 * 查询单个员工精简信息（跨模块契约）
	 *
	 * @param userId 用户ID
	 * @return 员工精简信息；用户不存在时返回 null
	 */
	UserBriefVO getBrief(Long userId);

	/**
	 * 判断用户是否拥有指定角色中的任意一个（跨模块契约）
	 * <p>
	 * 订单模块用它做经理/财务越权校验。本方法**只读本模块的角色数据**，
	 * 不依赖 M2/M3 的任何类型，避免环形依赖。
	 *
	 * @param userId    用户ID
	 * @param roleCodes 允许的角色编码，满足其一即返回 true
	 * @return 拥有其中任一角色时返回 true；用户不存在或入参为空时返回 false
	 */
	boolean hasAnyRole(Long userId, String... roleCodes);
}
