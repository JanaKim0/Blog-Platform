package com.blogplatform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

		@NotBlank(message = "Username is required")
		@Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
		@Pattern(regexp = "^[A-Za-z0-9._-]+$",
				message = "Username may contain only letters, digits, dots, underscores and hyphens")
		String username,

		@NotBlank(message = "Email is required")
		@Email(message = "Email is not valid")
		@Size(max = 254, message = "Email is too long")
		String email,

		// 72 bytes is where BCrypt stops reading, so a longer password would be
		// silently truncated.
		@NotBlank(message = "Password is required")
		@Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
		String password,

		@Size(max = 100, message = "Display name is too long")
		String displayName) {
}
