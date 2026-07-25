package com.blogplatform.web;

import com.blogplatform.dto.CommentRequest;
import com.blogplatform.dto.CommentResponse;
import com.blogplatform.dto.PageResponse;
import com.blogplatform.security.AppUserDetails;
import com.blogplatform.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comments hang off an article when they are created or listed, but an existing
 * comment is addressed by its own id - the article it belongs to is already
 * known from the id, and repeating it in the path would let the two disagree.
 */
@RestController
public class CommentController {

	private final CommentService commentService;

	public CommentController(CommentService commentService) {
		this.commentService = commentService;
	}

	@GetMapping("/api/articles/{slug}/comments")
	public PageResponse<CommentResponse> list(@PathVariable String slug,
			@AuthenticationPrincipal AppUserDetails principal,
			@PageableDefault(size = 20, sort = "createdAt",
					direction = Sort.Direction.ASC) Pageable pageable) {
		return commentService.list(slug, principal == null ? null : principal.getId(), pageable);
	}

	@PostMapping("/api/articles/{slug}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public CommentResponse add(@PathVariable String slug,
			@AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody CommentRequest request) {
		return commentService.add(principal.getId(), slug, request);
	}

	@PutMapping("/api/comments/{id}")
	public CommentResponse update(@PathVariable Long id,
			@AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody CommentRequest request) {
		return commentService.update(id, principal.getId(), request);
	}

	@DeleteMapping("/api/comments/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id,
			@AuthenticationPrincipal AppUserDetails principal) {
		commentService.delete(id, principal.getId(), principal.isAdmin());
	}
}
