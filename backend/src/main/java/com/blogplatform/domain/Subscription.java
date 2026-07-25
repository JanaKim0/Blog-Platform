package com.blogplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * "{@code follower} follows {@code author}". Directed, so following someone
 * back is a second row.
 */
@Entity
@Table(name = "subscriptions",
		uniqueConstraints = @UniqueConstraint(name = "uk_subscriptions_follower_author",
				columnNames = {"follower_id", "author_id"}),
		indexes = {
				@Index(name = "idx_subscriptions_follower", columnList = "follower_id"),
				@Index(name = "idx_subscriptions_author", columnList = "author_id")
		})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Subscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** The user who pressed "Follow". */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "follower_id", nullable = false)
	private User follower;

	/** The author being followed. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
