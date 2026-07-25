package com.blogplatform.service;

import com.blogplatform.domain.Article;
import com.blogplatform.domain.Comment;
import com.blogplatform.dto.CommentRequest;
import com.blogplatform.dto.CommentResponse;
import com.blogplatform.dto.PageResponse;
import com.blogplatform.exception.ResourceNotFoundException;
import com.blogplatform.repository.CommentRepository;
import com.blogplatform.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Comments under an article. */
@Service
public class CommentService {

	private final CommentRepository comments;
	private final UserRepository users;
	private final ArticleLookup articleLookup;

	public CommentService(CommentRepository comments, UserRepository users,
			ArticleLookup articleLookup) {
		this.comments = comments;
		this.users = users;
		this.articleLookup = articleLookup;
	}

	@Transactional
	public CommentResponse add(Long authorId, String slug, CommentRequest request) {
		Article article = articleLookup.requirePublished(slug, authorId);

		Comment comment = new Comment();
		comment.setArticle(article);
		comment.setAuthor(users.getReferenceById(authorId));
		comment.setContent(request.content().trim());
		return CommentResponse.from(comments.saveAndFlush(comment));
	}

	/**
	 * Oldest first: a discussion reads top to bottom, and a stable order means a
	 * new comment does not shuffle the page somebody is already reading.
	 */
	@Transactional(readOnly = true)
	public PageResponse<CommentResponse> list(String slug, Long currentUserId, Pageable pageable) {
		Article article = articleLookup.requireVisible(slug, currentUserId);
		return PageResponse.from(comments.findByArticleId(article.getId(), pageable),
				CommentResponse::from);
	}

	/**
	 * Only the person who wrote a comment may change its text - not the article's
	 * author and not an administrator. Rewriting somebody else's words is
	 * something no amount of authority should allow; moderation is deletion.
	 */
	@Transactional
	public CommentResponse update(Long commentId, Long currentUserId, CommentRequest request) {
		Comment comment = require(commentId);
		if (!comment.getAuthor().getId().equals(currentUserId)) {
			throw new AccessDeniedException("Only the author of a comment can edit it");
		}
		comment.setContent(request.content().trim());
		return CommentResponse.from(comments.saveAndFlush(comment));
	}

	/**
	 * Deletion is open to the comment's author, the author of the article (so
	 * they can moderate their own page) and administrators.
	 */
	@Transactional
	public void delete(Long commentId, Long currentUserId, boolean admin) {
		Comment comment = require(commentId);
		boolean ownComment = comment.getAuthor().getId().equals(currentUserId);
		boolean ownArticle = comment.getArticle().getAuthor().getId().equals(currentUserId);

		if (!admin && !ownComment && !ownArticle) {
			throw new AccessDeniedException("You are not allowed to delete this comment");
		}
		comments.delete(comment);
	}

	private Comment require(Long commentId) {
		return comments.findById(commentId)
				.orElseThrow(() -> ResourceNotFoundException.of("Comment", commentId));
	}
}
