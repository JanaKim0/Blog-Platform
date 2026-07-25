package com.blogplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A reader's comment under an article. */
@Entity
@Table(name = "comments", indexes = {
		@Index(name = "idx_comments_article", columnList = "article_id"),
		@Index(name = "idx_comments_author", columnList = "author_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Comment extends AuditableEntity {

	public static final int MAX_CONTENT_LENGTH = 2_000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "article_id", nullable = false)
	private Article article;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Column(nullable = false, length = MAX_CONTENT_LENGTH)
	private String content;
}
