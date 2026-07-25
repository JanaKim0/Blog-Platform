package com.blogplatform.web;

import com.blogplatform.dto.ArticleRequest;
import com.blogplatform.dto.ArticleResponse;
import com.blogplatform.dto.ArticleSearchRequest;
import com.blogplatform.dto.ArticleSort;
import com.blogplatform.dto.ArticleSummaryResponse;
import com.blogplatform.dto.PageResponse;
import com.blogplatform.security.AppUserDetails;
import com.blogplatform.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Articles are addressed by their slug, which is fixed when the article is
 * created and never rewritten - that is what makes it usable in links.
 */
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

	private final ArticleService articleService;

	public ArticleController(ArticleService articleService) {
		this.articleService = articleService;
	}

	/**
	 * The public feed of published articles. Every filter is optional, so the
	 * bare URL is the "latest publications" list.
	 *
	 * @param sortBy is deliberately not called {@code sort} - that name belongs to
	 *               Spring's pageable resolver, and the two would collide
	 */
	@GetMapping
	public PageResponse<ArticleSummaryResponse> list(
			@RequestParam(required = false) String query,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String tag,
			@RequestParam(required = false) String author,
			@RequestParam(required = false) ArticleSort sortBy,
			@AuthenticationPrincipal AppUserDetails principal,
			@PageableDefault(size = 10) Pageable pageable) {
		ArticleSearchRequest request =
				new ArticleSearchRequest(query, category, tag, author, sortBy);
		return articleService.search(request, principal == null ? null : principal.getId(),
				pageable);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ArticleResponse create(@AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody ArticleRequest request) {
		return articleService.create(principal.getId(), request);
	}

	/** Public for published articles; a draft is only visible to its author. */
	@GetMapping("/{slug}")
	public ArticleResponse read(@PathVariable String slug,
			@AuthenticationPrincipal AppUserDetails principal) {
		return articleService.getBySlug(slug, principal == null ? null : principal.getId());
	}

	@PutMapping("/{slug}")
	public ArticleResponse update(@PathVariable String slug,
			@AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody ArticleRequest request) {
		return articleService.update(slug, principal.getId(), principal.isAdmin(), request);
	}

	@DeleteMapping("/{slug}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable String slug,
			@AuthenticationPrincipal AppUserDetails principal) {
		articleService.delete(slug, principal.getId(), principal.isAdmin());
	}

	@PostMapping("/{slug}/cover")
	public ArticleResponse uploadCover(@PathVariable String slug,
			@AuthenticationPrincipal AppUserDetails principal,
			@RequestPart("file") MultipartFile file) {
		return articleService.setCover(slug, principal.getId(), principal.isAdmin(), file);
	}

	@DeleteMapping("/{slug}/cover")
	public ArticleResponse removeCover(@PathVariable String slug,
			@AuthenticationPrincipal AppUserDetails principal) {
		return articleService.removeCover(slug, principal.getId(), principal.isAdmin());
	}
}
