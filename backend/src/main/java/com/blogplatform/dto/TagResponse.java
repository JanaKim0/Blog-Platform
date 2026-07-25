package com.blogplatform.dto;

import com.blogplatform.domain.Tag;

public record TagResponse(Long id, String name, String slug) {

	public static TagResponse from(Tag tag) {
		return new TagResponse(tag.getId(), tag.getName(), tag.getSlug());
	}
}
