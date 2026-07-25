package com.blogplatform.service;

import com.blogplatform.domain.Article;
import com.blogplatform.domain.ArticleStatus;
import com.blogplatform.dto.ArticleRequest;
import com.blogplatform.dto.ArticleResponse;
import com.blogplatform.dto.ArticleSearchRequest;
import com.blogplatform.dto.ArticleSummaryResponse;
import com.blogplatform.dto.PageResponse;
import com.blogplatform.exception.ResourceNotFoundException;
import com.blogplatform.repository.ArticleLikeRepository;
import com.blogplatform.repository.ArticleRepository;
import com.blogplatform.repository.CommentRepository;
import com.blogplatform.repository.SubscriptionRepository;
import com.blogplatform.repository.UserRepository;
import com.blogplatform.util.Slugs;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** Writing, editing and removing articles. */
@Service
public class ArticleService {

	/** Longest slug we derive from a title; the column allows a little more. */
	private static final int MAX_SLUG_LENGTH = 200;

	private final ArticleRepository articles;
	private final UserRepository users;
	private final CommentRepository comments;
	private final ArticleLikeRepository likes;
	private final SubscriptionRepository subscriptions;
	private final CategoryService categoryService;
	private final TagService tagService;
	private final ImageStorageService imageStorage;
	private final ArticleAssembler assembler;

	public ArticleService(ArticleRepository articles, UserRepository users,
			CommentRepository comments, ArticleLikeRepository likes,
			SubscriptionRepository subscriptions, CategoryService categoryService,
			TagService tagService, ImageStorageService imageStorage, ArticleAssembler assembler) {
		this.articles = articles;
		this.users = users;
		this.comments = comments;
		this.likes = likes;
		this.subscriptions = subscriptions;
		this.categoryService = categoryService;
		this.tagService = tagService;
		this.imageStorage = imageStorage;
		this.assembler = assembler;
	}

	@Transactional
	public ArticleResponse create(Long authorId, ArticleRequest request) {
		Article article = new Article();
		article.setAuthor(users.findById(authorId)
				.orElseThrow(() -> ResourceNotFoundException.of("User", authorId)));
		article.setSlug(uniqueSlug(request.title()));
		apply(article, request);
		return assembler.toResponse(save(article), authorId);
	}

	/**
	 * Reads one article. Drafts answer 404 for everyone but their author - a 403
	 * would confirm that the article exists, which is exactly what a draft should
	 * not do.
	 */
	@Transactional(readOnly = true)
	public ArticleResponse getBySlug(String slug, Long currentUserId) {
		Article article = requireBySlug(slug);
		if (!article.isPublished() && !isAuthor(article, currentUserId)) {
			throw new ResourceNotFoundException("No article at " + slug);
		}
		return assembler.toResponse(article, currentUserId);
	}

	/**
	 * The slug is left alone on purpose: it was derived from the original title
	 * and every link to the article uses it, so rewriting it would break them.
	 */
	@Transactional
	public ArticleResponse update(String slug, Long currentUserId, boolean admin,
			ArticleRequest request) {
		Article article = requireBySlug(slug);
		requireCanModify(article, currentUserId, admin);
		apply(article, request);
		return assembler.toResponse(articles.save(article), currentUserId);
	}

	@Transactional
	public void delete(String slug, Long currentUserId, boolean admin) {
		Article article = requireBySlug(slug);
		requireCanModify(article, currentUserId, admin);

		// Comments and likes point at the article, so they go first. Doing this
		// here rather than with JPA cascades avoids loading every child row.
		comments.deleteByArticleId(article.getId());
		likes.deleteByArticleId(article.getId());
		String coverUrl = article.getCoverUrl();
		articles.delete(article);
		imageStorage.delete(coverUrl);
	}

	@Transactional
	public ArticleResponse setCover(String slug, Long currentUserId, boolean admin,
			MultipartFile file) {
		Article article = requireBySlug(slug);
		requireCanModify(article, currentUserId, admin);

		String previous = article.getCoverUrl();
		article.setCoverUrl(imageStorage.store(file, ImageKind.COVER));
		ArticleResponse response = assembler.toResponse(articles.save(article), currentUserId);
		imageStorage.delete(previous);
		return response;
	}

	@Transactional
	public ArticleResponse removeCover(String slug, Long currentUserId, boolean admin) {
		Article article = requireBySlug(slug);
		requireCanModify(article, currentUserId, admin);

		String previous = article.getCoverUrl();
		article.setCoverUrl(null);
		ArticleResponse response = assembler.toResponse(articles.save(article), currentUserId);
		imageStorage.delete(previous);
		return response;
	}

	/** Everything the signed-in author has written, drafts included. */
	@Transactional(readOnly = true)
	public PageResponse<ArticleSummaryResponse> myArticles(Long authorId, Pageable pageable) {
		return loadPage(articles.findIdsByAuthorId(authorId, pageable), authorId);
	}

	/**
	 * The public article list: published articles, optionally narrowed by text,
	 * category, tag or author, and ordered by date or popularity.
	 */
	@Transactional(readOnly = true)
	public PageResponse<ArticleSummaryResponse> search(ArticleSearchRequest request,
			Long currentUserId, Pageable pageable) {
		Page<Long> ids = articles.searchIds(
				ArticleStatus.PUBLISHED,
				likePattern(request.query()),
				blankToNull(request.category()),
				blankToNull(request.tag()),
				lowercaseOrNull(request.author()),
				request.sortOrDefault().name(),
				unsorted(pageable));
		return loadPage(ids, currentUserId);
	}

	/**
	 * The reader's own timeline: published articles by the authors they follow.
	 * Following nobody gives an empty page rather than the whole site, so the UI
	 * can prompt them to follow someone.
	 */
	@Transactional(readOnly = true)
	public PageResponse<ArticleSummaryResponse> feed(Long currentUserId, Pageable pageable) {
		List<Long> followedAuthors = subscriptions.findFollowedAuthorIds(currentUserId);
		if (followedAuthors.isEmpty()) {
			return PageResponse.from(Page.empty(pageable));
		}
		return loadPage(
				articles.feedIds(ArticleStatus.PUBLISHED, followedAuthors, unsorted(pageable)),
				currentUserId);
	}

	// --- internals ------------------------------------------------------

	/**
	 * Turns a page of ids into a page of DTOs. {@code in (:ids)} does not
	 * preserve order, so the articles are put back into the order the ids came
	 * in - that order is the sort the caller asked for.
	 */
	private PageResponse<ArticleSummaryResponse> loadPage(Page<Long> ids, Long currentUserId) {
		if (ids.isEmpty()) {
			return PageResponse.from(Page.empty(ids.getPageable()));
		}
		Map<Long, Article> byId = articles.findAllWithDetails(ids.getContent()).stream()
				.collect(Collectors.toMap(Article::getId, Function.identity()));
		List<Article> ordered = ids.getContent().stream()
				.map(byId::get)
				.filter(Objects::nonNull)
				.toList();
		Page<Article> page = new PageImpl<>(ordered, ids.getPageable(), ids.getTotalElements());
		return assembler.toPage(page, currentUserId);
	}

	/**
	 * Drops any {@code sort} the client sent. Ordering for these lists lives in
	 * the query itself, and an extra ORDER BY on a grouped query would clash
	 * with it.
	 */
	private Pageable unsorted(Pageable pageable) {
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
	}

	private String likePattern(String query) {
		return StringUtils.hasText(query)
				? "%" + query.trim().toLowerCase(Locale.ROOT) + "%"
				: null;
	}

	private String lowercaseOrNull(String value) {
		return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
	}

	private String blankToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private void apply(Article article, ArticleRequest request) {
		article.setTitle(request.title().trim());
		article.setSummary(StringUtils.hasText(request.summary()) ? request.summary().trim() : null);
		article.setContent(request.content());
		article.setCategory(categoryService.resolveOrNull(request.categorySlug()));

		article.getTags().clear();
		article.getTags().addAll(tagService.resolve(request.tags()));

		article.setStatus(request.status());
		// Stamped once, on the first publish, so re-publishing a draft does not
		// move an old article to the top of the feed.
		if (request.status() == ArticleStatus.PUBLISHED && article.getPublishedAt() == null) {
			article.setPublishedAt(Instant.now());
		}
	}

	private Article save(Article article) {
		try {
			return articles.saveAndFlush(article);
		}
		catch (DataIntegrityViolationException ex) {
			// Two articles with the same title saved at the same moment: the
			// unique index rejected the loser, so give it its own suffix.
			article.setSlug(article.getSlug() + "-" + UUID.randomUUID().toString().substring(0, 6));
			return articles.saveAndFlush(article);
		}
	}

	/** {@code my-title}, or {@code my-title-2} when that one is taken. */
	private String uniqueSlug(String title) {
		String base = Slugs.slugify(title, "article", MAX_SLUG_LENGTH);
		String candidate = base;
		int suffix = 2;
		while (articles.existsBySlug(candidate)) {
			candidate = base + "-" + suffix++;
		}
		return candidate;
	}

	private Article requireBySlug(String slug) {
		return articles.findBySlug(slug)
				.orElseThrow(() -> new ResourceNotFoundException("No article at " + slug));
	}

	private void requireCanModify(Article article, Long currentUserId, boolean admin) {
		if (!admin && !isAuthor(article, currentUserId)) {
			throw new AccessDeniedException("Only the author can change this article");
		}
	}

	private boolean isAuthor(Article article, Long userId) {
		return userId != null && article.getAuthor().getId().equals(userId);
	}
}
