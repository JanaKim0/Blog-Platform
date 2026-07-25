package com.blogplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A broad topic an article belongs to. An article has at most one category,
 * unlike tags, of which it can have many.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 80)
	private String name;

	/** URL-friendly form of the name, e.g. {@code web-development}. */
	@Column(nullable = false, unique = true, length = 100)
	private String slug;

	@Column(length = 255)
	private String description;
}
