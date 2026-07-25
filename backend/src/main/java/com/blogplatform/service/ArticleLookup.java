package com.blogplatform.service;

import com.blogplatform.domain.Article;
import com.blogplatform.exception.BadRequestException;
import com.blogplatform.exception.ResourceNotFoundException;
import com.blogplatform.repository.ArticleRepository;
import org.springframework.stereotype.Component;

/**
 * Finds articles by slug and applies the visibility rules, so that every
 * service - articles, comments, likes - answers the same way for a draft or a
 * missing article instead of each inventing its own behaviour.
 */
@Component
public class ArticleLookup {

	private final ArticleRepository articles;

	public ArticleLookup(ArticleRepository articles) {
		this.articles = articles;
	}

	/** The article regardless of status; ownership is the caller's business. */
	public Article require(String slug) {
		return articles.findBySlug(slug)
				.orElseThrow(() -> new ResourceNotFoundException("No article at " + slug));
	}

	/**
	 * The article as this caller is allowed to see it. A draft belonging to
	 * somebody else answers 404 rather than 403 - a 403 would confirm that it
	 * exists.
	 */
	public Article requireVisible(String slug, Long currentUserId) {
		Article article = require(slug);
		if (!article.isPublished() && !isAuthor(article, currentUserId)) {
			throw new ResourceNotFoundException("No article at " + slug);
		}
		return article;
	}

	/**
	 * The article, and it must be published: commenting on or liking a draft is
	 * not something the author should be able to do either, so they get a plain
	 * explanation while everyone else still gets 404.
	 */
	public Article requirePublished(String slug, Long currentUserId) {
		Article article = requireVisible(slug, currentUserId);
		if (!article.isPublished()) {
			throw new BadRequestException("This article is still a draft");
		}
		return article;
	}

	public boolean isAuthor(Article article, Long userId) {
		return userId != null && article.getAuthor().getId().equals(userId);
	}
}
