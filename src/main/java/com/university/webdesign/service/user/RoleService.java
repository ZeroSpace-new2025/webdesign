package com.university.webdesign.service.user;

import com.university.webdesign.common.PageResult;
import com.university.webdesign.service.user.dto.PermVO;
import com.university.webdesign.service.user.dto.RoleCmd;
import com.university.webdesign.service.user.dto.RoleQuery;
import com.university.webdesign.service.user.dto.RoleVO;

import java.util.List;
import java.util.Set;

/**
 * 角色与权限维护服务。
 * <p>
 * 对应《对外方法表》5.2 的 {@code RoleService}：角色增删改查、权限点字典查询、
 * 角色-权限映射的全量覆盖。
 * <p>
 * 身份传递：写操作由实现类通过
 * {@link com.university.webdesign.common.UserContextHolder#require()} 取当前登录用户后校验角色。
 */
public interface RoleService
{
	/**
	 * 新增角色（M4-13）
	 * <p>
	 * 角色编码唯一，冲突抛 40901。
	 *
	 * @param cmd 角色入参
	 * @return 新角色ID
	 */
	Long create(RoleCmd cmd);

	/**
	 * 分页查询角色（M4-14）
	 *
	 * @param q 查询条件
	 * @return 分页角色列表
	 */
	PageResult<RoleVO> page(RoleQuery q);

	/**
	 * 更新角色名称与描述（M4-15）
	 * <p>
	 * 发布 {@link com.university.webdesign.event.RolePermissionChangedEvent} 使鉴权缓存失效；
	 * 角色编码不可修改。
	 *
	 * @param roleId 角色ID
	 * @param cmd    角色入参
	 */
	void update(Long roleId, RoleCmd cmd);

	/**
	 * 删除角色（M4-16）
	 * <p>
	 * 存在关联用户时拒绝并抛 40902。
	 *
	 * @param roleId 角色ID
	 */
	void delete(Long roleId);

	/**
	 * 查询权限点字典（M4-17）
	 *
	 * @param module 模块名，为空表示全部模块
	 * @return 权限点列表，按模块与编码排序
	 */
	List<PermVO> listPermissions(String module);

	/**
	 * 配置角色权限（M4-18），全量覆盖
	 * <p>
	 * 发布 {@link com.university.webdesign.event.RolePermissionChangedEvent}。
	 *
	 * @param roleId    角色ID
	 * @param permCodes 权限点编码集合，为空表示清空
	 */
	void updatePermissions(Long roleId, List<String> permCodes);

	/**
	 * 查询角色已授权限（M4-19）
	 *
	 * @param roleId 角色ID
	 * @return 权限点编码集合
	 */
	Set<String> getPermissions(Long roleId);
}
