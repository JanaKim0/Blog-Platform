package com.blogplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Categories are a curated taxonomy, so only administrators send this. */
public record CategoryRequest(

		@NotBlank(message = "Name is required")
		@Size(max = 80, message = "Name is too long")
		String name,

		@Size(max = 255, message = "Description is too long")
		String description) {
}
