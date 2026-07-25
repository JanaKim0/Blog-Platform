package com.blogplatform.dto;

import com.blogplatform.domain.Role;
import com.blogplatform.domain.User;
import java.time.Instant;

/** The signed-in user's own account, including the fields only they may see. */
public record CurrentUserResponse(Long id, String username, String email, String displayName,
		String bio, String avatarUrl, Role role, Instant createdAt) {

	public static CurrentUserResponse from(User user) {
		return new CurrentUserResponse(user.getId(), user.getUsername(), user.getEmail(),
				user.resolveDisplayName(), user.getBio(), user.getAvatarUrl(), user.getRole(),
				user.getCreatedAt());
	}
}
