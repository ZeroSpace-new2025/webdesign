package com.university.webdesign.config;

import com.university.webdesign.domain.menu.Recipe;
import com.university.webdesign.domain.menu.RecipeStatus;
import com.university.webdesign.repository.menu.RecipeRepository;
import com.university.webdesign.service.menu.MenuService;
import com.university.webdesign.service.menu.RecipeService;
import com.university.webdesign.service.menu.dto.MenuCreateCmd;
import com.university.webdesign.service.menu.dto.MenuCreateItem;
import com.university.webdesign.service.menu.dto.RecipeCreateCmd;
import com.university.webdesign.service.menu.dto.RecipeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 首次启动时的演示菜单初始化（只做这一件事）。
 * <p>
 * <b>预置角色、演示账号、默认时间窗口都不在本类里</b>：它们的唯一来源是
 * {@code reploy/sql/seed.sql}（角色名 + 权限位图、BCrypt 密码、09:00/11:30）。
 * <ul>
 *     <li>{@code dev} profile：Hibernate 建表后由 {@code spring.sql.init} 自动执行该脚本
 *         （配置见 {@code application.yml} 的 dev 段）；</li>
 *     <li>PostgreSQL 部署：由 DBA 手工 {@code psql -f reploy/sql/seed.sql}，应用启动时不写数据。</li>
 * </ul>
 * 这样“系统里有哪些角色、各自有什么权限”只维护在 SQL 里，改角色不需要重新编译。
 * <p>
 * 演示菜品与当日菜单留在 Java 里，是因为发布菜单必须走 {@link MenuService#publish(Long)}：
 * 要先写 {@code menu_snapshot} 冻结菜名/分类/单位/单价，纯 INSERT 无法保证这条业务不变量。
 * <p>
 * 幂等：菜品按名称复用（菜品名全局唯一，重复创建会抛 40901），当日已有已发布菜单就跳过；
 * 菜品全部下架时不建空菜单。只在 {@code app.storage=database}（默认）时生效。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage", havingValue = "database", matchIfMissing = true)
public class DatabaseDataInitializer implements CommandLineRunner
{
	private final RecipeRepository recipeRepository;
	private final RecipeService recipeService;
	private final MenuService menuService;

	public DatabaseDataInitializer(
			RecipeRepository recipeRepository,
			RecipeService recipeService,
			MenuService menuService) {
		this.recipeRepository = recipeRepository;
		this.recipeService = recipeService;
		this.menuService = menuService;
	}

	@Override
	@Transactional
	public void run(String... args) {
		seedMenu();
	}

	/**
	 * 写入演示菜品并发布当日菜单，让点餐页与下单链路开箱可用
	 * <p>
	 * 幂等：菜品名全局唯一，已存在的同名菜品直接复用，不再重复创建
	 * （否则演示菜单被下架后重启会撞 40901 而中断启动）；当日没有任何可用菜品时不建空菜单。
	 */
	private void seedMenu() {
		if (menuService.getCurrent(LocalDate.now()) != null) {
			return;
		}
		Map<String, String[]> dishes = new LinkedHashMap<>();
		dishes.put("黑椒牛柳", new String[] {"热菜", "份", "32.00"});
		dishes.put("宫保鸡丁", new String[] {"热菜", "份", "26.00"});
		dishes.put("香煎鳕鱼", new String[] {"热菜", "份", "36.00"});
		dishes.put("青椒肉丝", new String[] {"热菜", "份", "22.00"});
		dishes.put("米饭", new String[] {"主食", "份", "2.00"});
		dishes.put("杂粮饭", new String[] {"主食", "份", "3.00"});
		dishes.put("例汤", new String[] {"汤品", "份", "4.00"});
		dishes.put("酸奶", new String[] {"饮品", "份", "6.00"});

		MenuCreateCmd menu = new MenuCreateCmd();
		menu.setName("今日菜单 " + LocalDate.now());
		menu.setDescription("系统初始化的演示菜单，可直接用于点餐与下单联调");
		menu.setEffectiveDate(LocalDate.now());
		List<MenuCreateItem> items = new ArrayList<>();
		for (Map.Entry<String, String[]> entry : dishes.entrySet()) {
			Long recipeId = ensureRecipe(entry.getKey(), entry.getValue());
			RecipeVO saved = recipeService.getById(recipeId);
			if (!RecipeStatus.ACTIVE.name().equals(saved.getStatus())) {
				continue;
			}
			MenuCreateItem item = new MenuCreateItem();
			item.setRecipeId(recipeId);
			items.add(item);
		}
		if (items.isEmpty()) {
			log.warn("没有可用的演示菜品（可能已全部下架），跳过演示菜单初始化");
			return;
		}
		menu.setItems(items);
		Long menuId = menuService.createDraft(menu);
		menuService.publish(menuId);
		log.info("已初始化演示菜单（菜单ID={}，菜品 {} 道）", menuId, items.size());
	}

	/**
	 * 取菜品ID：已存在同名菜品时直接复用，不存在才创建
	 *
	 * @param name 菜品名称（全局唯一）
	 * @param meta 分类 / 单位 / 单价
	 * @return 菜品ID
	 */
	private Long ensureRecipe(String name, String[] meta) {
		Optional<Recipe> existing = recipeRepository.findByRecipeName(name);
		if (existing.isPresent()) {
			return existing.get().getRecipeId();
		}
		RecipeCreateCmd recipe = new RecipeCreateCmd();
		recipe.setName(name);
		recipe.setCategory(meta[0]);
		recipe.setUnit(meta[1]);
		recipe.setUnitPrice(new BigDecimal(meta[2]));
		recipe.setDescription("演示菜品：" + name);
		return recipeService.create(recipe);
	}
}
