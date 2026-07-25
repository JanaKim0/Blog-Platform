package com.blogplatform.dto;

import com.blogplatform.domain.User;
import java.time.Instant;

/**
 * An author's public profile. Deliberately without the email address: this is
 * readable by anyone, signed in or not.
 *
 * @param following whether the caller follows this author - always false for
 *                  anonymous visitors and for one's own profile
 */
public record ProfileResponse(Long id, String username, String displayName, String bio,
		String avatarUrl, Instant joinedAt, long articlesCount, long followersCount,
		long followingCount, boolean following) {

	public static ProfileResponse of(User user, long articlesCount, long followersCount,
			long followingCount, boolean following) {
		return new ProfileResponse(user.getId(), user.getUsername(), user.resolveDisplayName(),
				user.getBio(), user.getAvatarUrl(), user.getCreatedAt(), articlesCount,
				followersCount, followingCount, following);
	}
}
