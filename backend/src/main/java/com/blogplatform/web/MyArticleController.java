package com.blogplatform.web;

import com.blogplatform.dto.ArticleSummaryResponse;
import com.blogplatform.dto.PageResponse;
import com.blogplatform.security.AppUserDetails;
import com.blogplatform.service.ArticleService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The author's own article list, drafts included.
 * <p>
 * It lives under {@code /api/me} rather than {@code /api/articles/mine} so that
 * it can never be shadowed by - or shadow - an article whose slug happens to be
 * "mine".
 */
@RestController
@RequestMapping("/api/me")
public class MyArticleController {

	private final ArticleService articleService;

	public MyArticleController(ArticleService articleService) {
		this.articleService = articleService;
	}

	@GetMapping("/articles")
	public PageResponse<ArticleSummaryResponse> myArticles(
			@AuthenticationPrincipal AppUserDetails principal,
			@PageableDefault(size = 20, sort = "updatedAt",
					direction = Sort.Direction.DESC) Pageable pageable) {
		return articleService.myArticles(principal.getId(), pageable);
	}

	/** The reader's timeline: published articles by the authors they follow. */
	@GetMapping("/feed")
	public PageResponse<ArticleSummaryResponse> feed(
			@AuthenticationPrincipal AppUserDetails principal,
			@PageableDefault(size = 10) Pageable pageable) {
		return articleService.feed(principal.getId(), pageable);
	}
}
