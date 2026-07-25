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
 * A free-form label attached to articles. Tags are created on demand when an
 * author types a new one, and shared between all articles that use them.
 */
@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
public class Tag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String name;

	@Column(nullable = false, unique = true, length = 60)
	private String slug;
}
