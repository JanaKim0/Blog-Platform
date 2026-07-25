package com.blogplatform.dto;

import com.blogplatform.domain.Article;
import com.blogplatform.domain.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Used for both creating and updating an article.
 *
 * @param categorySlug the slug of an existing category, or {@code null} for none
 * @param tags         free-form tag names; unknown ones are created on the spot
 */
public record ArticleRequest(

		@NotBlank(message = "Title is required")
		@Size(max = 200, message = "Title must be at most 200 characters")
		String title,

		@Size(max = 500, message = "Summary must be at most 500 characters")
		String summary,

		@NotBlank(message = "Content is required")
		@Size(max = Article.MAX_CONTENT_LENGTH, message = "The article is too long")
		String content,

		String categorySlug,

		@Size(max = 10, message = "An article can have at most 10 tags")
		List<@Size(max = 50, message = "A tag must be at most 50 characters") String> tags,

		@NotNull(message = "Status is required")
		ArticleStatus status) {
}
