package com.blogplatform.dto;

import com.blogplatform.domain.Comment;
import java.time.Instant;

/**
 * @param edited whether the comment has been changed since it was posted, so the
 *               UI can say so instead of quietly showing different words
 */
public record CommentResponse(Long id, String content, UserSummary author, Instant createdAt,
		Instant updatedAt, boolean edited) {

	public static CommentResponse from(Comment comment) {
		return new CommentResponse(comment.getId(), comment.getContent(),
				UserSummary.from(comment.getAuthor()), comment.getCreatedAt(),
				comment.getUpdatedAt(), comment.getUpdatedAt().isAfter(comment.getCreatedAt()));
	}
}
