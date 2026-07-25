package com.blogplatform.dto;

import jakarta.validation.constraints.NotBlank;

/** Either the username or the email address is accepted as the login. */
public record LoginRequest(

		@NotBlank(message = "Username or email is required")
		String login,

		@NotBlank(message = "Password is required")
		String password) {
}
