package com.blogplatform.dto;

import com.blogplatform.domain.ArticleStatus;
import java.time.Instant;
import java.util.List;

/** A full article, body included - what the article page renders. */
public record ArticleResponse(Long id, String slug, String title, String summary, String content,
		String coverUrl, UserSummary author, CategoryResponse category, List<TagResponse> tags,
		ArticleStatus status, Instant publishedAt, Instant createdAt, Instant updatedAt,
		long likesCount, long commentsCount, boolean likedByMe) {
}
