package com.blogplatform.service;

import com.blogplatform.domain.Article;
import com.blogplatform.domain.Tag;
import com.blogplatform.dto.ArticleResponse;
import com.blogplatform.dto.ArticleSummaryResponse;
import com.blogplatform.dto.CategoryResponse;
import com.blogplatform.dto.PageResponse;
import com.blogplatform.dto.TagResponse;
import com.blogplatform.dto.UserSummary;
import com.blogplatform.repository.ArticleLikeRepository;
import com.blogplatform.repository.CommentRepository;
import com.blogplatform.repository.IdCount;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Builds article DTOs, including the like and comment counts.
 * <p>
 * The counts are the reason this exists: fetching them per article would mean
 * two extra queries for every card on the page. Instead a whole page is
 * assembled with three queries in total - one for the like counts, one for the
 * comment counts, and one for "which of these did I like".
 */
@Component
public class ArticleAssembler {

	private final ArticleLikeRepository likes;
	private final CommentRepository comments;

	public ArticleAssembler(ArticleLikeRepository likes, CommentRepository comments) {
		this.likes = likes;
		this.comments = comments;
	}

	public PageResponse<ArticleSummaryResponse> toPage(Page<Article> page, Long currentUserId) {
		Counts counts = countsFor(page.getContent(), currentUserId);
		return PageResponse.from(page, article -> toSummary(article, counts));
	}

	public List<ArticleSummaryResponse> toSummaries(List<Article> articles, Long currentUserId) {
		Counts counts = countsFor(articles, currentUserId);
		return articles.stream().map(article -> toSummary(article, counts)).toList();
	}

	public ArticleResponse toResponse(Article article, Long currentUserId) {
		Counts counts = countsFor(List.of(article), currentUserId);
		return new ArticleResponse(article.getId(), article.getSlug(), article.getTitle(),
				article.getSummary(), article.getContent(), article.getCoverUrl(),
				UserSummary.from(article.getAuthor()), category(article), tags(article),
				article.getStatus(), article.getPublishedAt(), article.getCreatedAt(),
				article.getUpdatedAt(), counts.likes(article.getId()),
				counts.comments(article.getId()), counts.liked(article.getId()));
	}

	private ArticleSummaryResponse toSummary(Article article, Counts counts) {
		return new ArticleSummaryResponse(article.getId(), article.getSlug(), article.getTitle(),
				article.getSummary(), article.getCoverUrl(), UserSummary.from(article.getAuthor()),
				category(article), tags(article), article.getStatus(), article.getPublishedAt(),
				article.getUpdatedAt(), counts.likes(article.getId()),
				counts.comments(article.getId()), counts.liked(article.getId()));
	}

	private CategoryResponse category(Article article) {
		return article.getCategory() == null ? null : CategoryResponse.from(article.getCategory());
	}

	private List<TagResponse> tags(Article article) {
		return article.getTags().stream()
				.sorted(Comparator.comparing(Tag::getName))
				.map(TagResponse::from)
				.toList();
	}

	private Counts countsFor(List<Article> articles, Long currentUserId) {
		List<Long> ids = articles.stream().map(Article::getId).toList();
		if (ids.isEmpty()) {
			return new Counts(Map.of(), Map.of(), Set.of());
		}
		return new Counts(
				toMap(likes.countByArticleIds(ids)),
				toMap(comments.countByArticleIds(ids)),
				currentUserId == null
						? Set.of()
						: new HashSet<>(likes.findLikedArticleIds(currentUserId, ids)));
	}

	private Map<Long, Long> toMap(List<IdCount> rows) {
		return rows.stream().collect(Collectors.toMap(IdCount::id, IdCount::count,
				(first, second) -> first, HashMap::new));
	}

	/** The three lookups a page of articles needs, already resolved. */
	private record Counts(Map<Long, Long> likesByArticle, Map<Long, Long> commentsByArticle,
			Set<Long> likedByCurrentUser) {

		long likes(Long articleId) {
			return likesByArticle.getOrDefault(articleId, 0L);
		}

		long comments(Long articleId) {
			return commentsByArticle.getOrDefault(articleId, 0L);
		}

		boolean liked(Long articleId) {
			return likedByCurrentUser.contains(articleId);
		}
	}
}
