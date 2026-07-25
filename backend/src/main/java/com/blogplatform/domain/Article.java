package com.blogplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A blog post. Drafts are visible only to their author; publishing stamps
 * {@link #publishedAt}, which is what the public feed is ordered by.
 */
@Entity
@Table(name = "articles", indexes = {
		@Index(name = "idx_articles_author", columnList = "author_id"),
		@Index(name = "idx_articles_category", columnList = "category_id"),
		@Index(name = "idx_articles_status_published_at", columnList = "status, published_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Article extends AuditableEntity {

	/** Longest article body we accept, in characters. */
	public static final int MAX_CONTENT_LENGTH = 100_000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** URL-friendly identifier derived from the title, e.g. {@code my-first-post-7}. */
	@Column(nullable = false, unique = true, length = 280)
	private String slug;

	@Column(nullable = false, length = 200)
	private String title;

	/** Short teaser shown in feed cards. */
	@Column(length = 500)
	private String summary;

	@Column(nullable = false, length = MAX_CONTENT_LENGTH)
	private String content;

	/** Relative URL of the uploaded cover image. */
	@Column(name = "cover_url", length = 255)
	private String coverUrl;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "article_tags",
			joinColumns = @JoinColumn(name = "article_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id"))
	private Set<Tag> tags = new LinkedHashSet<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ArticleStatus status = ArticleStatus.DRAFT;

	/** Set the first time the article is published, then left alone. */
	@Column(name = "published_at")
	private Instant publishedAt;

	public boolean isPublished() {
		return status == ArticleStatus.PUBLISHED;
	}
}
