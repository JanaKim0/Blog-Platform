package com.blogplatform.dto;

import com.blogplatform.domain.ArticleStatus;
import java.time.Instant;
import java.util.List;

/**
 * An article as it appears in a feed card: everything except the body.
 *
 * @param likedByMe false for anonymous readers
 */
public record ArticleSummaryResponse(Long id, String slug, String title, String summary,
		String coverUrl, UserSummary author, CategoryResponse category, List<TagResponse> tags,
		ArticleStatus status, Instant publishedAt, Instant updatedAt, long likesCount,
		long commentsCount, boolean likedByMe) {
}
