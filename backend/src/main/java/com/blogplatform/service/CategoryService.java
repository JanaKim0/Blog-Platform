package com.blogplatform.service;

import com.blogplatform.domain.Category;
import com.blogplatform.dto.CategoryRequest;
import com.blogplatform.dto.CategoryResponse;
import com.blogplatform.exception.ConflictException;
import com.blogplatform.exception.ResourceNotFoundException;
import com.blogplatform.repository.ArticleRepository;
import com.blogplatform.repository.CategoryRepository;
import com.blogplatform.util.Slugs;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Categories are a curated list rather than something every author invents, so
 * reading them is public and changing them is an administrator's job.
 */
@Service
public class CategoryService {

	private final CategoryRepository categories;
	private final ArticleRepository articles;

	public CategoryService(CategoryRepository categories, ArticleRepository articles) {
		this.categories = categories;
		this.articles = articles;
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> list() {
		return categories.findAllByOrderByNameAsc().stream().map(CategoryResponse::from).toList();
	}

	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		String name = request.name().trim();
		if (categories.findByNameIgnoreCase(name).isPresent()) {
			throw new ConflictException("A category named \"" + name + "\" already exists");
		}
		String slug = Slugs.slugify(name, "category", 100);
		if (categories.existsBySlug(slug)) {
			throw new ConflictException("A category with the slug \"" + slug + "\" already exists");
		}

		Category category = new Category();
		category.setName(name);
		category.setSlug(slug);
		category.setDescription(StringUtils.hasText(request.description())
				? request.description().trim() : null);
		return CategoryResponse.from(categories.save(category));
	}

	@Transactional
	public void delete(String slug) {
		Category category = requireBySlug(slug);
		// Articles keep existing; they simply lose their category.
		articles.clearCategory(category.getId());
		categories.delete(category);
	}

	/** Resolves the slug an author picked, or {@code null} when none was given. */
	@Transactional(readOnly = true)
	public Category resolveOrNull(String slug) {
		return StringUtils.hasText(slug) ? requireBySlug(slug) : null;
	}

	private Category requireBySlug(String slug) {
		return categories.findBySlug(slug)
				.orElseThrow(() -> new ResourceNotFoundException("No category named " + slug));
	}
}
