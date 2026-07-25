package com.blogplatform.repository;

import com.blogplatform.domain.Article;
import com.blogplatform.domain.ArticleStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Listing queries here return <em>ids</em>, not entities, and the ids are then
 * turned into fully loaded articles by {@link #findAllWithDetails}.
 * <p>
 * The reason is that paginating a query which also fetches a collection
 * (an article's tags) forces Hibernate to read every matching row and apply the
 * page in memory. Splitting it in two keeps the pagination in SQL and still
 * loads authors, categories and tags without an extra query per article.
 */
public interface ArticleRepository extends JpaRepository<Article, Long> {

	/** Loads author, category and tags in one go - used by the article page. */
	@EntityGraph(attributePaths = {"author", "category", "tags"})
	Optional<Article> findBySlug(String slug);

	boolean existsBySlug(String slug);

	long countByAuthorIdAndStatus(Long authorId, ArticleStatus status);

	/** Fully loaded articles for an already paginated list of ids. */
	@EntityGraph(attributePaths = {"author", "category", "tags"})
	@Query("select a from Article a where a.id in :ids")
	List<Article> findAllWithDetails(@Param("ids") Collection<Long> ids);

	/**
	 * The public article list: only published articles, with every filter
	 * optional. A {@code null} parameter switches its condition off, which keeps
	 * one query instead of one per combination of filters.
	 * <p>
	 * The category and the tags are joined <em>explicitly</em> with a left join.
	 * Writing {@code a.category.slug} instead would let Hibernate add an inner
	 * join, and every article without a category would silently disappear from
	 * the list.
	 *
	 * @param status the status to list, always {@code PUBLISHED} here. It is a
	 *               bound parameter rather than a literal in the query so that
	 *               Hibernate uses the attribute's own mapping - as a literal it
	 *               is compared as an ordinal and silently matches nothing,
	 *               because the column stores the name
	 * @param query  already lowercased and wrapped in {@code %}, or null
	 * @param sortBy {@code LIKES}, {@code COMMENTS} or anything else for newest
	 *               first
	 */
	@Query(value = """
			select a.id from Article a
			join a.author author
			left join a.category category
			left join a.tags tag
			where a.status = :status
			  and (:query is null
			       or lower(a.title) like :query
			       or lower(coalesce(a.summary, '')) like :query
			       or lower(a.content) like :query)
			  and (:categorySlug is null or category.slug = :categorySlug)
			  and (:tagSlug is null or tag.slug = :tagSlug)
			  and (:authorUsername is null or lower(author.username) = :authorUsername)
			group by a.id, a.publishedAt
			order by
			  case
			    when :sortBy = 'LIKES'
			      then (select count(l) from ArticleLike l where l.article.id = a.id)
			    when :sortBy = 'COMMENTS'
			      then (select count(c) from Comment c where c.article.id = a.id)
			    else 0
			  end desc,
			  a.publishedAt desc,
			  a.id desc
			""",
			countQuery = """
			select count(distinct a.id) from Article a
			join a.author author
			left join a.category category
			left join a.tags tag
			where a.status = :status
			  and (:query is null
			       or lower(a.title) like :query
			       or lower(coalesce(a.summary, '')) like :query
			       or lower(a.content) like :query)
			  and (:categorySlug is null or category.slug = :categorySlug)
			  and (:tagSlug is null or tag.slug = :tagSlug)
			  and (:authorUsername is null or lower(author.username) = :authorUsername)
			""")
	Page<Long> searchIds(@Param("status") ArticleStatus status,
			@Param("query") String query,
			@Param("categorySlug") String categorySlug,
			@Param("tagSlug") String tagSlug,
			@Param("authorUsername") String authorUsername,
			@Param("sortBy") String sortBy,
			Pageable pageable);

	/** Published articles by the authors the reader follows, newest first. */
	@Query("""
			select a.id from Article a
			where a.status = :status
			  and a.author.id in :authorIds
			order by a.publishedAt desc, a.id desc
			""")
	Page<Long> feedIds(@Param("status") ArticleStatus status,
			@Param("authorIds") Collection<Long> authorIds, Pageable pageable);

	/** Everything one author wrote, drafts included. */
	@Query("select a.id from Article a where a.author.id = :authorId")
	Page<Long> findIdsByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

	/**
	 * Detaches every article from a category that is being deleted. The articles
	 * stay; they simply become uncategorised.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Article a set a.category = null where a.category.id = :categoryId")
	void clearCategory(@Param("categoryId") Long categoryId);
}
