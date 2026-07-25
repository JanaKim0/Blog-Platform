package com.blogplatform.service;

import com.blogplatform.domain.Article;
import com.blogplatform.domain.ArticleLike;
import com.blogplatform.dto.ArticleResponse;
import com.blogplatform.repository.ArticleLikeRepository;
import com.blogplatform.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liking and unliking articles. Both are idempotent, for the same reason
 * following an author is: the heart in the UI is a state, so pressing it twice
 * should not be an error.
 */
@Service
public class ArticleLikeService {

	private final ArticleLikeRepository likes;
	private final UserRepository users;
	private final ArticleLookup articleLookup;
	private final ArticleAssembler assembler;

	public ArticleLikeService(ArticleLikeRepository likes, UserRepository users,
			ArticleLookup articleLookup, ArticleAssembler assembler) {
		this.likes = likes;
		this.users = users;
		this.articleLookup = articleLookup;
		this.assembler = assembler;
	}

	@Transactional
	public ArticleResponse like(Long userId, String slug) {
		Article article = articleLookup.requirePublished(slug, userId);

		if (!likes.existsByArticleIdAndUserId(article.getId(), userId)) {
			ArticleLike like = new ArticleLike();
			like.setArticle(article);
			like.setUser(users.getReferenceById(userId));
			try {
				likes.saveAndFlush(like);
			}
			catch (DataIntegrityViolationException ex) {
				// Two taps at once: the unique constraint rejected the second,
				// and the article is liked either way.
			}
		}
		return assembler.toResponse(article, userId);
	}

	@Transactional
	public ArticleResponse unlike(Long userId, String slug) {
		Article article = articleLookup.requirePublished(slug, userId);
		likes.findByArticleIdAndUserId(article.getId(), userId).ifPresent(likes::delete);
		likes.flush();
		return assembler.toResponse(article, userId);
	}
}
