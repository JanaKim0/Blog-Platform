package com.blogplatform.config;

import com.blogplatform.domain.Category;
import com.blogplatform.repository.CategoryRepository;
import com.blogplatform.util.Slugs;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Puts a starting taxonomy in place on an empty database, because authors pick a
 * category from a list rather than inventing one. It only ever runs when the
 * table is empty, so an administrator's later edits are never overwritten.
 */
@Component
public class CategorySeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(CategorySeeder.class);

	private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

	static {
		DEFAULTS.put("Programming", "Code, tools and everything around them");
		DEFAULTS.put("Design", "Interfaces, typography and visual craft");
		DEFAULTS.put("Career", "Work, studying and growing as a professional");
		DEFAULTS.put("Science", "Research, discoveries and how things work");
		DEFAULTS.put("Travel", "Places, routes and what they are like");
		DEFAULTS.put("Food", "Recipes, ingredients and cooking");
		DEFAULTS.put("Life", "Personal notes and everything else");
	}

	private final CategoryRepository categories;

	public CategorySeeder(CategoryRepository categories) {
		this.categories = categories;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (categories.count() > 0) {
			return;
		}
		DEFAULTS.forEach((name, description) -> {
			Category category = new Category();
			category.setName(name);
			category.setSlug(Slugs.slugify(name, "category", 100));
			category.setDescription(description);
			categories.save(category);
		});
		log.info("Seeded {} default categories", DEFAULTS.size());
	}
}
