package com.blogplatform.repository;

import com.blogplatform.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	@EntityGraph(attributePaths = {"author"})
	Page<Comment> findByArticleId(Long articleId, Pageable pageable);

	long countByArticleId(Long articleId);

	long countByAuthorId(Long authorId);

	/** Called before an article is removed, since comments reference it. */
	void deleteByArticleId(Long articleId);
}
