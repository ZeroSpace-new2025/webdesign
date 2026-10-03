package com.university.webdesign.support;

import com.university.webdesign.menurecipe.api.MenuDTO;
import com.university.webdesign.menurecipe.api.MenuQueryData;
import com.university.webdesign.menurecipe.api.RecipeDTO;
import com.university.webdesign.menurecipe.api.RecipeQueryData;
import com.university.webdesign.menurecipe.service.MenuService;
import com.university.webdesign.menurecipe.service.RecipeService;
import com.university.webdesign.user.api.LoginData;
import com.university.webdesign.user.api.PasswordUpdateData;
import com.university.webdesign.user.api.UserDTO;
import com.university.webdesign.user.api.UserImportData;
import com.university.webdesign.user.api.UserImportResultDTO;
import com.university.webdesign.user.api.UserQueryData;
import com.university.webdesign.user.api.UserRegisterData;
import com.university.webdesign.user.api.UserUpdateData;
import com.university.webdesign.user.service.UserService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.util.List;

/**
 * 测试用桩服务配置
 * <p>
 * 菜品与菜单中心、用户与报表中心的 service 实现尚未提供，而它们的 Controller 已经存在，
 * 导致 Spring 上下文缺少必需 Bean。这里为测试提供最小桩实现，
 * 使上下文能够启动（生产代码不受影响）。
 * <p>
 * //todo 确认：各模块实现类落地后，本桩配置即可删除，
 * 改为在测试中直接注入真实实现或用 {@code @MockitoBean} 按需替换。
 */
@TestConfiguration
public class StubServicesTestConfiguration
{
	/**
	 * 是否授予所有角色，供需要经理/财务权限的测试切换
	 */
	private static volatile boolean ALL_ROLES = false;
	
	/**
	 * 菜单服务桩：默认返回“无生效菜单”，需要菜单的测试自行用 {@code @MockitoBean} 替换。
	 *
	 * @return 菜单服务桩
	 */
	@Bean
	public MenuService stubMenuService() {
		return new MenuService()
		{
			@Override
			public MenuDTO getActiveMenu(LocalDate date) {
				return null;
			}
			
			@Override
			public MenuDTO getMenu() {
				return new MenuDTO();
			}
			
			@Override
			public List<MenuDTO> query(MenuQueryData queryData) {
				return List.of();
			}
			
			@Override
			public List<MenuDTO> getAllMenus() {
				return List.of();
			}
		};
	}
	
	/**
	 * 菜谱服务桩
	 *
	 * @return 菜谱服务桩
	 */
	@Bean
	public RecipeService stubRecipeService() {
		return new RecipeService()
		{
			@Override
			public RecipeDTO getRecipe(Long recipeId) {
				return new RecipeDTO();
			}
			
			@Override
			public List<RecipeDTO> getAllRecipes() {
				return List.of();
			}
			
			@Override
			public List<RecipeDTO> query(RecipeQueryData queryData) {
				return List.of();
			}
		};
	}
	
	/**
	 * 用户服务桩：登录与员工维护类方法返回空值，不落库；
	 * 角色与权限判定由 {@link #grantAllRoles(boolean)} 控制。
	 *
	 * @return 用户服务桩
	 */
	@Bean
	public UserService stubUserService() {
		return new UserService()
		{
			@Override
			public UserDTO login(LoginData loginData) {
				return null;
			}
			
			@Override
			public void logout() {
				// 桩实现：无登录态可注销
			}
			
			@Override
			public UserDTO createUser(UserRegisterData registerData) {
				return null;
			}
			
			@Override
			public List<UserDTO> query(UserQueryData queryData) {
				return List.of();
			}
			
			@Override
			public UserDTO getUserInfo(Long userId) {
				return new UserDTO();
			}
			
			@Override
			public UserDTO updateUser(UserUpdateData updateData) {
				return null;
			}
			
			@Override
			public void changePassword(PasswordUpdateData passwordUpdateData) {
				// 桩实现：不落库
			}
			
			@Override
			public void resetPassword(Long userId, String newPassword) {
				// 桩实现：不落库
			}
			
			@Override
			public void setEnabled(Long userId, boolean enabled) {
				// 桩实现：不落库
			}
			
			@Override
			public void assignRoles(Long userId, List<Long> roleIds) {
				// 桩实现：不落库
			}
			
			@Override
			public void deleteUser(Long userId) {
				// 桩实现：不落库
			}
			
			@Override
			public UserImportResultDTO importUsers(UserImportData importData) {
				return new UserImportResultDTO();
			}
			
			@Override
			public List<String> getPermissionCodes(Long userId) {
				// 最小权限：默认不授予任何权限，需要授权的测试显式调用 grantAllRoles(true)
				return ALL_ROLES && userId != null ? List.of("*") : List.of();
			}
			
			@Override
			public boolean hasPermission(Long userId, String permissionCode) {
				return userId != null && ALL_ROLES;
			}
			
			@Override
			public boolean hasAnyRole(Long userId, String... roleCodes) {
				// 最小权限：默认不授予任何角色，需要授权角色的测试显式调用 grantAllRoles(true)
				return userId != null && ALL_ROLES;
			}
		};
	}
	
	/**
	 * 开关“授予所有角色”
	 * <p>
	 * 用户中心的实现尚未提供，测试无法通过真实数据造出经理角色，这里提供显式开关。
	 * 用完记得在测试结束（{@code @AfterEach}）复位，避免影响其他测试。
	 *
	 * @param granted true 表示所有用户都拥有任意角色
	 */
	public static void grantAllRoles(boolean granted) {
		ALL_ROLES = granted;
	}
}
