package com.blogplatform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Editable parts of one's own profile. The username is not among them - it
 * appears in profile URLs, so changing it would break every link to the author.
 */
public record UpdateProfileRequest(

		@Size(max = 100, message = "Display name is too long")
		String displayName,

		@Size(max = 1000, message = "Bio must be at most 1000 characters")
		String bio,

		@NotBlank(message = "Email is required")
		@Email(message = "Email is not valid")
		@Size(max = 254, message = "Email is too long")
		String email) {
}
