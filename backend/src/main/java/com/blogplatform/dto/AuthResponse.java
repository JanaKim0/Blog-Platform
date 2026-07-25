package com.blogplatform.dto;

import java.time.Instant;

/** What registering or signing in returns: a token plus who you now are. */
public record AuthResponse(String token, Instant expiresAt, CurrentUserResponse user) {
}
