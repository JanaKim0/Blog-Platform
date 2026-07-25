package com.blogplatform.dto;

import com.blogplatform.domain.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(

		@NotBlank(message = "A comment cannot be empty")
		@Size(max = Comment.MAX_CONTENT_LENGTH, message = "The comment is too long")
		String content) {
}
