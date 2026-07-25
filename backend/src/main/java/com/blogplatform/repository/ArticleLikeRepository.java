package com.blogplatform.repository;

import com.blogplatform.domain.ArticleLike;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {

	Optional<ArticleLike> findByArticleIdAndUserId(Long articleId, Long userId);

	boolean existsByArticleIdAndUserId(Long articleId, Long userId);

	long countByArticleId(Long articleId);

	void deleteByArticleId(Long articleId);

	/**
	 * Ids of the articles in {@code articleIds} that this user has liked - one
	 * query for a whole page of feed cards instead of one per card.
	 */
	@Query("select l.article.id from ArticleLike l where l.user.id = :userId and l.article.id in :articleIds")
	List<Long> findLikedArticleIds(@Param("userId") Long userId,
			@Param("articleIds") Collection<Long> articleIds);

	/** Like counts for a whole page of articles in one query. */
	@Query("""
			select new com.blogplatform.repository.IdCount(l.article.id, count(l))
			from ArticleLike l
			where l.article.id in :articleIds
			group by l.article.id
			""")
	List<IdCount> countByArticleIds(@Param("articleIds") Collection<Long> articleIds);
}
