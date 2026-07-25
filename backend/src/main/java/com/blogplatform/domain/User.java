package com.blogplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A registered account. Authors, commenters and followers are all users - the
 * relationships between them live in {@link Article}, {@link Comment} and
 * {@link Subscription} rather than in collections on this entity, so that
 * loading a user never drags in their whole blog.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Login name, also used in profile URLs. */
	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(nullable = false, unique = true, length = 254)
	private String email;

	/** BCrypt hash - never the raw password. */
	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	/** Name shown next to articles and comments; falls back to the username. */
	@Column(name = "display_name", length = 100)
	private String displayName;

	@Column(length = 1000)
	private String bio;

	/** Relative URL of the uploaded avatar, e.g. {@code /uploads/ab/cd12.jpg}. */
	@Column(name = "avatar_url", length = 255)
	private String avatarUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role = Role.USER;

	@Column(nullable = false)
	private boolean enabled = true;

	/** The name to show in the UI: the display name if set, otherwise the login. */
	public String resolveDisplayName() {
		return displayName == null || displayName.isBlank() ? username : displayName;
	}
}
