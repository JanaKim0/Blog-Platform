package com.blogplatform.web;

import com.blogplatform.dto.ArticleResponse;
import com.blogplatform.security.AppUserDetails;
import com.blogplatform.service.ArticleLikeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Both actions answer with the whole article, so the caller gets the new like
 * count and the new state of its own like without a second request.
 */
@RestController
@RequestMapping("/api/articles/{slug}/like")
public class ArticleLikeController {

	private final ArticleLikeService likeService;

	public ArticleLikeController(ArticleLikeService likeService) {
		this.likeService = likeService;
	}

	@PostMapping
	public ArticleResponse like(@PathVariable String slug,
			@AuthenticationPrincipal AppUserDetails principal) {
		return likeService.like(principal.getId(), slug);
	}

	@DeleteMapping
	public ArticleResponse unlike(@PathVariable String slug,
			@AuthenticationPrincipal AppUserDetails principal) {
		return likeService.unlike(principal.getId(), slug);
	}
}
