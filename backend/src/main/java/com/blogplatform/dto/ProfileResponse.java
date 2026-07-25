package com.blogplatform.dto;

import com.blogplatform.domain.User;
import java.time.Instant;

/**
 * An author's public profile. Deliberately without the email address: this is
 * readable by anyone, signed in or not.
 */
public record ProfileResponse(Long id, String username, String displayName, String bio,
		String avatarUrl, Instant joinedAt, long articlesCount, long followersCount,
		long followingCount) {

	public static ProfileResponse of(User user, long articlesCount, long followersCount,
			long followingCount) {
		return new ProfileResponse(user.getId(), user.getUsername(), user.resolveDisplayName(),
				user.getBio(), user.getAvatarUrl(), user.getCreatedAt(), articlesCount,
				followersCount, followingCount);
	}
}
