package com.university.webdesign.menurecipe;

import com.university.webdesign.menurecipe.data.Recipe;
import com.university.webdesign.menurecipe.data.RecipeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 菜单模块示例数据初始化。
 * 仅在菜品库为空时插入一些演示菜品，方便快速查看效果。
 */
@Component
@Order(1)
public class MenuDataLoader implements CommandLineRunner {

	private final RecipeRepository recipeRepository;

	public MenuDataLoader(RecipeRepository recipeRepository) {
		this.recipeRepository = recipeRepository;
	}

	@Override
	public void run(String... args) {
		if (recipeRepository.count() > 0) {
			return;
		}
		List<Recipe> samples = List.of(
				recipe("小炒肉", "荤菜", "份", new BigDecimal("12.00"), "经典湘菜，下饭首选"),
				recipe("番茄炒蛋", "荤菜", "份", new BigDecimal("8.00"), "酸甜开胃"),
				recipe("清炒时蔬", "素菜", "份", new BigDecimal("6.00"), "当日新鲜绿叶菜"),
				recipe("米饭", "主食", "两", new BigDecimal("1.00"), "东北大米"),
				recipe("紫菜蛋花汤", "汤品", "份", new BigDecimal("4.00"), "清淡鲜汤"),
				recipe("红烧排骨", "荤菜", "份", new BigDecimal("15.00"), "酥烂入味")
		);
		recipeRepository.saveAll(samples);
	}

	private static Recipe recipe(String name, String category, String unit, BigDecimal price, String desc) {
		Recipe r = new Recipe();
		r.setRecipeName(name);
		r.setCategory(category);
		r.setUnit(unit);
		r.setPrice(price);
		r.setDescription(desc);
		r.setStatus("ACTIVE");
		r.setCreatedBy(1L);
		return r;
	}
}
