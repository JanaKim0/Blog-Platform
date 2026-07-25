package com.blogplatform.repository;

import com.blogplatform.domain.Article;
import com.blogplatform.domain.ArticleStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Long> {

	/** Loads author, category and tags in one go - used by the article page. */
	@EntityGraph(attributePaths = {"author", "category", "tags"})
	Optional<Article> findBySlug(String slug);

	boolean existsBySlug(String slug);

	@EntityGraph(attributePaths = {"author", "category", "tags"})
	Page<Article> findByStatus(ArticleStatus status, Pageable pageable);

	@EntityGraph(attributePaths = {"author", "category", "tags"})
	Page<Article> findByAuthorIdAndStatus(Long authorId, ArticleStatus status, Pageable pageable);

	/** Every article of one author, drafts included - for "my articles". */
	@EntityGraph(attributePaths = {"author", "category", "tags"})
	Page<Article> findByAuthorId(Long authorId, Pageable pageable);

	long countByAuthorIdAndStatus(Long authorId, ArticleStatus status);

	/**
	 * Detaches every article from a category that is being deleted. The articles
	 * stay; they simply become uncategorised.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Article a set a.category = null where a.category.id = :categoryId")
	void clearCategory(@Param("categoryId") Long categoryId);
}
