package com.blogplatform.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blogplatform.ApiIntegrationTest;
import com.blogplatform.domain.Article;
import com.blogplatform.domain.ArticleLike;
import com.blogplatform.domain.Comment;
import com.blogplatform.domain.User;
import com.blogplatform.repository.ArticleLikeRepository;
import com.blogplatform.repository.ArticleRepository;
import com.blogplatform.repository.CommentRepository;
import com.blogplatform.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * The public article list: filtering, searching, sorting and pagination.
 * <p>
 * Likes and comments are created straight through the repositories here, because
 * the endpoints that create them come later; what is under test is the ordering,
 * not the way a like is recorded.
 */
class ArticleFeedTests extends ApiIntegrationTest {

	@Autowired
	private ArticleRepository articles;
	@Autowired
	private UserRepository users;
	@Autowired
	private ArticleLikeRepository likes;
	@Autowired
	private CommentRepository comments;

	// --- the latest publications ----------------------------------------

	@Test
	void listsPublishedArticlesNewestFirst() throws Exception {
		String token = register("nora");
		create(token, "First Post", "PUBLISHED", null, null);
		create(token, "Second Post", "PUBLISHED", null, null);
		create(token, "Third Post", "PUBLISHED", null, null);

		mockMvc.perform(get("/api/articles"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.content[0].title").value("Third Post"))
				.andExpect(jsonPath("$.content[2].title").value("First Post"))
				// feed cards carry no body
				.andExpect(jsonPath("$.content[0].content").doesNotExist());
	}

	@Test
	void keepsDraftsOutOfThePublicList() throws Exception {
		String token = register("nora");
		create(token, "Published", "PUBLISHED", null, null);
		create(token, "Hidden", "DRAFT", null, null);

		mockMvc.perform(get("/api/articles"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].title").value("Published"));
	}

	@Test
	void splitsTheListIntoPages() throws Exception {
		String token = register("nora");
		create(token, "One", "PUBLISHED", null, null);
		create(token, "Two", "PUBLISHED", null, null);
		create(token, "Three", "PUBLISHED", null, null);

		mockMvc.perform(get("/api/articles").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(false));

		mockMvc.perform(get("/api/articles").param("size", "2").param("page", "1"))
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.first").value(false))
				.andExpect(jsonPath("$.last").value(true));
	}

	// --- filters --------------------------------------------------------

	@Test
	void filtersByCategory() throws Exception {
		String token = register("nora");
		create(token, "About Code", "PUBLISHED", "programming", null);
		create(token, "About Food", "PUBLISHED", "food", null);

		mockMvc.perform(get("/api/articles").param("category", "programming"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].title").value("About Code"));
	}

	@Test
	void filtersByTag() throws Exception {
		String token = register("nora");
		create(token, "With Java", "PUBLISHED", null, "[\"Java\",\"Spring\"]");
		create(token, "With Design", "PUBLISHED", null, "[\"Figma\"]");

		mockMvc.perform(get("/api/articles").param("tag", "java"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].title").value("With Java"));
	}

	@Test
	void doesNotDuplicateAnArticleThatHasSeveralTags() throws Exception {
		String token = register("nora");
		create(token, "Many Tags", "PUBLISHED", null, "[\"Java\",\"Spring\",\"JPA\"]");

		mockMvc.perform(get("/api/articles"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content.length()").value(1));
	}

	@Test
	void filtersByAuthor() throws Exception {
		String nora = register("nora");
		String mira = register("mira");
		create(nora, "By Nora", "PUBLISHED", null, null);
		create(mira, "By Mira", "PUBLISHED", null, null);

		mockMvc.perform(get("/api/articles").param("author", "NORA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].author.username").value("nora"));
	}

	@Test
	void combinesSeveralFilters() throws Exception {
		String nora = register("nora");
		String mira = register("mira");
		create(nora, "Nora on code", "PUBLISHED", "programming", "[\"Java\"]");
		create(nora, "Nora on food", "PUBLISHED", "food", "[\"Java\"]");
		create(mira, "Mira on code", "PUBLISHED", "programming", "[\"Java\"]");

		mockMvc.perform(get("/api/articles")
						.param("author", "nora").param("category", "programming")
						.param("tag", "java"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].title").value("Nora on code"));
	}

	// --- search ---------------------------------------------------------

	@Test
	void searchesTheTitleTheSummaryAndTheBody() throws Exception {
		String token = register("nora");
		createWithBody(token, "Gardening", "How to grow tomatoes", "Water them often.");
		createWithBody(token, "Cooking", "A summary about tomatoes", "Boil water.");
		createWithBody(token, "Cycling", "Nothing to do with it", "Pump the tomatoes tyres.");
		createWithBody(token, "Knitting", "Wool and needles", "Nothing relevant here.");

		mockMvc.perform(get("/api/articles").param("query", "tomatoes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(3));
	}

	@Test
	void ignoresTheCaseOfTheSearchQuery() throws Exception {
		String token = register("nora");
		createWithBody(token, "Spring Boot Basics", "An introduction", "Body text.");

		mockMvc.perform(get("/api/articles").param("query", "SPRING boot"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void treatsABlankQueryAsNoFilter() throws Exception {
		String token = register("nora");
		create(token, "Anything", "PUBLISHED", null, null);

		mockMvc.perform(get("/api/articles").param("query", "   "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void returnsAnEmptyPageWhenNothingMatches() throws Exception {
		String token = register("nora");
		create(token, "Anything", "PUBLISHED", null, null);

		mockMvc.perform(get("/api/articles").param("query", "zzzzzz"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0))
				.andExpect(jsonPath("$.content.length()").value(0));
	}

	// --- sorting --------------------------------------------------------

	@Test
	void sortsByNumberOfLikes() throws Exception {
		String token = register("nora");
		register("mira");
		register("lena");
		String popular = create(token, "Popular", "PUBLISHED", null, null);
		String quiet = create(token, "Quiet", "PUBLISHED", null, null);
		create(token, "Ignored", "PUBLISHED", null, null);

		like(popular, "mira");
		like(popular, "lena");
		like(quiet, "mira");

		mockMvc.perform(get("/api/articles").param("sortBy", "LIKES"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].title").value("Popular"))
				.andExpect(jsonPath("$.content[0].likesCount").value(2))
				.andExpect(jsonPath("$.content[1].title").value("Quiet"))
				.andExpect(jsonPath("$.content[1].likesCount").value(1))
				.andExpect(jsonPath("$.content[2].title").value("Ignored"))
				.andExpect(jsonPath("$.content[2].likesCount").value(0));
	}

	@Test
	void sortsByNumberOfComments() throws Exception {
		String token = register("nora");
		register("mira");
		String discussed = create(token, "Discussed", "PUBLISHED", null, null);
		create(token, "Silent", "PUBLISHED", null, null);

		comment(discussed, "mira", "First");
		comment(discussed, "nora", "Second");

		mockMvc.perform(get("/api/articles").param("sortBy", "COMMENTS"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].title").value("Discussed"))
				.andExpect(jsonPath("$.content[0].commentsCount").value(2))
				.andExpect(jsonPath("$.content[1].commentsCount").value(0));
	}

	@Test
	void fallsBackToTheNewestFirstWhenNoSortIsGiven() throws Exception {
		String token = register("nora");
		create(token, "Older", "PUBLISHED", null, null);
		create(token, "Newer", "PUBLISHED", null, null);

		mockMvc.perform(get("/api/articles"))
				.andExpect(jsonPath("$.content[0].title").value("Newer"));
	}

	// --- the personal timeline ------------------------------------------

	@Test
	void showsOnlyArticlesFromFollowedAuthorsInTheFeed() throws Exception {
		String reader = register("mira");
		String followed = register("nora");
		String ignored = register("lena");
		create(followed, "From Nora", "PUBLISHED", null, null);
		create(ignored, "From Lena", "PUBLISHED", null, null);

		mockMvc.perform(post("/api/users/nora/follow").header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/me/feed").header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].title").value("From Nora"));
	}

	@Test
	void keepsDraftsOfFollowedAuthorsOutOfTheFeed() throws Exception {
		String reader = register("mira");
		String author = register("nora");
		create(author, "Published", "PUBLISHED", null, null);
		create(author, "Draft", "DRAFT", null, null);
		mockMvc.perform(post("/api/users/nora/follow").header("Authorization", "Bearer " + reader));

		mockMvc.perform(get("/api/me/feed").header("Authorization", "Bearer " + reader))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].title").value("Published"));
	}

	@Test
	void givesAnEmptyFeedToSomebodyWhoFollowsNobody() throws Exception {
		String reader = register("mira");
		String author = register("nora");
		create(author, "From Nora", "PUBLISHED", null, null);

		mockMvc.perform(get("/api/me/feed").header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void refusesTheFeedWithoutAToken() throws Exception {
		mockMvc.perform(get("/api/me/feed")).andExpect(status().isUnauthorized());
	}

	// --- helpers --------------------------------------------------------

	/** Creates an article and returns its slug. */
	private String create(String token, String title, String status, String categorySlug,
			String tagsJson) throws Exception {
		String category = categorySlug == null ? "null" : "\"" + categorySlug + "\"";
		String tags = tagsJson == null ? "[]" : tagsJson;
		String body = mockMvc.perform(post("/api/articles")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"%s","summary":"A teaser","content":"The body.",
								 "status":"%s","categorySlug":%s,"tags":%s}
								""".formatted(title, status, category, tags)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return readJson(body).get("slug").asText();
	}

	private String createWithBody(String token, String title, String summary, String content)
			throws Exception {
		String body = mockMvc.perform(post("/api/articles")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"%s","summary":"%s","content":"%s","status":"PUBLISHED"}
								""".formatted(title, summary, content)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return readJson(body).get("slug").asText();
	}

	private void like(String slug, String username) {
		ArticleLike like = new ArticleLike();
		like.setArticle(article(slug));
		like.setUser(user(username));
		likes.saveAndFlush(like);
	}

	private void comment(String slug, String username, String text) {
		Comment comment = new Comment();
		comment.setArticle(article(slug));
		comment.setAuthor(user(username));
		comment.setContent(text);
		comments.saveAndFlush(comment);
	}

	private Article article(String slug) {
		return articles.findBySlug(slug).orElseThrow();
	}

	private User user(String username) {
		return users.findByUsernameIgnoreCase(username).orElseThrow();
	}
}
