package com.blogplatform.dto;

import com.blogplatform.domain.User;

/**
 * The little bit of a user that shows up next to an article or a comment. Never
 * carries the email address or anything else private.
 */
public record UserSummary(Long id, String username, String displayName, String avatarUrl) {

	public static UserSummary from(User user) {
		return new UserSummary(user.getId(), user.getUsername(), user.resolveDisplayName(),
				user.getAvatarUrl());
	}
}
