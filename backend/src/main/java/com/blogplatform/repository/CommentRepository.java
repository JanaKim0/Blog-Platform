package com.blogplatform.repository;

import com.blogplatform.domain.Comment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	@EntityGraph(attributePaths = {"author"})
	Page<Comment> findByArticleId(Long articleId, Pageable pageable);

	long countByArticleId(Long articleId);

	long countByAuthorId(Long authorId);

	/** Called before an article is removed, since comments reference it. */
	void deleteByArticleId(Long articleId);

	/** Comment counts for a whole page of articles in one query. */
	@Query("""
			select new com.blogplatform.repository.IdCount(c.article.id, count(c))
			from Comment c
			where c.article.id in :articleIds
			group by c.article.id
			""")
	List<IdCount> countByArticleIds(@Param("articleIds") Collection<Long> articleIds);
}
