package com.university.webdesign.support;

import com.university.webdesign.menurecipe.api.MenuDTO;
import com.university.webdesign.menurecipe.api.MenuQueryData;
import com.university.webdesign.menurecipe.api.MenuService;
import com.university.webdesign.menurecipe.api.RecipeDTO;
import com.university.webdesign.menurecipe.api.RecipeQueryData;
import com.university.webdesign.menurecipe.api.RecipeService;
import com.university.webdesign.user.api.LoginData;
import com.university.webdesign.user.api.UserDTO;
import com.university.webdesign.user.api.UserRegisterData;
import com.university.webdesign.user.api.UserService;
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
	 * 用户服务桩：登录相关方法返回空值，{@code hasRole} 一律返回 false（最小权限）。
	 *
	 * @return 用户服务桩
	 */
	@Bean
	public UserService stubUserService() {
		return new UserService()
		{
			@Override
			public String login(LoginData loginData) {
				return null;
			}
			
			@Override
			public boolean logout() {
				return true;
			}
			
			@Override
			public Long register(UserRegisterData registerData) {
				return null;
			}
			
			@Override
			public boolean updatePassword(Long userId, String newPassword) {
				return false;
			}
			
			@Override
			public UserDTO getUserInfo(Long userId) {
				return new UserDTO();
			}
			
			@Override
			public boolean hasRole(Long userId, String roleCode) {
				return false;
			}
		};
	}
}
