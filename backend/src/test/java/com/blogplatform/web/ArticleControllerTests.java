package com.blogplatform.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blogplatform.ApiIntegrationTest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class ArticleControllerTests extends ApiIntegrationTest {

	// --- creating -------------------------------------------------------

	@Test
	void createsAnArticleAndDerivesTheSlugFromTheTitle() throws Exception {
		String token = register("nora");

		mockMvc.perform(post("/api/articles").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Hello World","summary":"A teaser","content":"The body.",
								 "status":"PUBLISHED"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.slug").value("hello-world"))
				.andExpect(jsonPath("$.title").value("Hello World"))
				.andExpect(jsonPath("$.content").value("The body."))
				.andExpect(jsonPath("$.author.username").value("nora"))
				.andExpect(jsonPath("$.status").value("PUBLISHED"))
				.andExpect(jsonPath("$.publishedAt").isNotEmpty())
				.andExpect(jsonPath("$.likesCount").value(0))
				.andExpect(jsonPath("$.commentsCount").value(0))
				.andExpect(jsonPath("$.likedByMe").value(false));
	}

	@Test
	void givesTwoArticlesWithTheSameTitleDifferentSlugs() throws Exception {
		String token = register("nora");

		assertThat(createArticle(token, "Hello World", "PUBLISHED")).isEqualTo("hello-world");
		assertThat(createArticle(token, "Hello World", "PUBLISHED")).isEqualTo("hello-world-2");
		assertThat(createArticle(token, "Hello World", "PUBLISHED")).isEqualTo("hello-world-3");
	}

	@Test
	void buildsAReadableSlugFromARussianTitle() throws Exception {
		String token = register("nora");

		assertThat(createArticle(token, "Как я учила Spring", "PUBLISHED"))
				.isEqualTo("kak-ya-uchila-spring");
	}

	@Test
	void leavesADraftWithoutAPublicationDate() throws Exception {
		String token = register("nora");

		mockMvc.perform(post("/api/articles").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Work in progress","content":"Half written.","status":"DRAFT"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.publishedAt").doesNotExist());
	}

	@Test
	void refusesToCreateAnArticleWithoutAToken() throws Exception {
		mockMvc.perform(post("/api/articles").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Sneaky","content":"Body.","status":"PUBLISHED"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void reportsMissingFields() throws Exception {
		String token = register("nora");

		mockMvc.perform(post("/api/articles").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"","content":""}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.title").isNotEmpty())
				.andExpect(jsonPath("$.fieldErrors.content").isNotEmpty())
				.andExpect(jsonPath("$.fieldErrors.status").isNotEmpty());
	}

	// --- categories and tags --------------------------------------------

	@Test
	void assignsACategoryAndCreatesUnknownTagsOnTheFly() throws Exception {
		String token = register("nora");

		mockMvc.perform(post("/api/articles").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"On Spring","content":"Body.","status":"PUBLISHED",
								 "categorySlug":"programming","tags":["Java","Spring Boot"]}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.category.slug").value("programming"))
				.andExpect(jsonPath("$.tags.length()").value(2))
				.andExpect(jsonPath("$.tags[0].name").value("Java"))
				.andExpect(jsonPath("$.tags[1].slug").value("spring-boot"));

		mockMvc.perform(get("/api/tags"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].slug")
						.value(org.hamcrest.Matchers.containsInAnyOrder("java", "spring-boot")));
	}

	@Test
	void treatsDifferentSpellingsOfATagAsOneTag() throws Exception {
		String token = register("nora");

		mockMvc.perform(post("/api/articles").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Tags","content":"Body.","status":"PUBLISHED",
								 "tags":["Spring Boot","spring boot","spring-boot","  "]}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tags.length()").value(1))
				.andExpect(jsonPath("$.tags[0].slug").value("spring-boot"));
	}

	@Test
	void refusesAnUnknownCategory() throws Exception {
		String token = register("nora");

		mockMvc.perform(post("/api/articles").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Nowhere","content":"Body.","status":"PUBLISHED",
								 "categorySlug":"does-not-exist"}
								"""))
				.andExpect(status().isNotFound());
	}

	// --- reading --------------------------------------------------------

	@Test
	void letsAnyoneReadAPublishedArticle() throws Exception {
		String token = register("nora");
		String slug = createArticle(token, "Public Post", "PUBLISHED");

		mockMvc.perform(get("/api/articles/" + slug))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Public Post"));
	}

	@Test
	void hidesADraftFromEveryoneButItsAuthor() throws Exception {
		String author = register("nora");
		String stranger = register("mira");
		String slug = createArticle(author, "Secret Draft", "DRAFT");

		// 404 rather than 403: a 403 would confirm the draft exists.
		mockMvc.perform(get("/api/articles/" + slug))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/articles/" + slug).header("Authorization", "Bearer " + stranger))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/articles/" + slug).header("Authorization", "Bearer " + author))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Secret Draft"));
	}

	@Test
	void answers404ForAnUnknownSlug() throws Exception {
		mockMvc.perform(get("/api/articles/nothing-here"))
				.andExpect(status().isNotFound());
	}

	// --- editing --------------------------------------------------------

	@Test
	void updatesTheArticleButKeepsTheOriginalSlug() throws Exception {
		String token = register("nora");
		String slug = createArticle(token, "First Title", "PUBLISHED");

		mockMvc.perform(put("/api/articles/" + slug).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"A Completely New Title","content":"Rewritten.",
								 "status":"PUBLISHED"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("A Completely New Title"))
				.andExpect(jsonPath("$.content").value("Rewritten."))
				// links to the article keep working
				.andExpect(jsonPath("$.slug").value("first-title"));
	}

	@Test
	void keepsThePublicationDateOfTheFirstPublish() throws Exception {
		String token = register("nora");
		String slug = createArticle(token, "Timeline", "PUBLISHED");

		String firstPublish = readJson(mockMvc.perform(get("/api/articles/" + slug))
				.andReturn().getResponse().getContentAsString()).get("publishedAt").asText();

		// back to draft, then published again
		update(token, slug, "Timeline", "DRAFT");
		String republished = readJson(update(token, slug, "Timeline", "PUBLISHED"))
				.get("publishedAt").asText();

		assertThat(republished).isEqualTo(firstPublish);
	}

	@Test
	void refusesEditsFromSomebodyElse() throws Exception {
		String author = register("nora");
		String stranger = register("mira");
		String slug = createArticle(author, "Mine", "PUBLISHED");

		mockMvc.perform(put("/api/articles/" + slug).header("Authorization", "Bearer " + stranger)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Hijacked","content":"Not yours.","status":"PUBLISHED"}
								"""))
				.andExpect(status().isForbidden());
	}

	// --- deleting -------------------------------------------------------

	@Test
	void deletesOwnArticle() throws Exception {
		String token = register("nora");
		String slug = createArticle(token, "Temporary", "PUBLISHED");

		mockMvc.perform(delete("/api/articles/" + slug).header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/articles/" + slug))
				.andExpect(status().isNotFound());
	}

	@Test
	void refusesDeletionFromSomebodyElse() throws Exception {
		String author = register("nora");
		String stranger = register("mira");
		String slug = createArticle(author, "Keep", "PUBLISHED");

		mockMvc.perform(delete("/api/articles/" + slug)
						.header("Authorization", "Bearer " + stranger))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/articles/" + slug)).andExpect(status().isOk());
	}

	// --- cover image ----------------------------------------------------

	@Test
	void uploadsAndRemovesACoverImage() throws Exception {
		String token = register("nora");
		String slug = createArticle(token, "Illustrated", "PUBLISHED");

		mockMvc.perform(multipart("/api/articles/" + slug + "/cover")
						.file(pngUpload()).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.coverUrl").isNotEmpty());

		mockMvc.perform(delete("/api/articles/" + slug + "/cover")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.coverUrl").doesNotExist());
	}

	@Test
	void refusesACoverUploadFromSomebodyElse() throws Exception {
		String author = register("nora");
		String stranger = register("mira");
		String slug = createArticle(author, "Illustrated", "PUBLISHED");

		mockMvc.perform(multipart("/api/articles/" + slug + "/cover")
						.file(pngUpload()).header("Authorization", "Bearer " + stranger))
				.andExpect(status().isForbidden());
	}

	// --- the author's own list ------------------------------------------

	@Test
	void listsTheAuthorsOwnArticlesIncludingDrafts() throws Exception {
		String token = register("nora");
		createArticle(token, "Published One", "PUBLISHED");
		createArticle(token, "Draft One", "DRAFT");

		mockMvc.perform(get("/api/me/articles").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[*].status")
						.value(org.hamcrest.Matchers.containsInAnyOrder("PUBLISHED", "DRAFT")));
	}

	@Test
	void refusesTheOwnArticleListWithoutAToken() throws Exception {
		mockMvc.perform(get("/api/me/articles")).andExpect(status().isUnauthorized());
	}

	@Test
	void countsOnlyPublishedArticlesOnTheProfile() throws Exception {
		String token = register("nora");
		createArticle(token, "Published One", "PUBLISHED");
		createArticle(token, "Draft One", "DRAFT");

		mockMvc.perform(get("/api/users/nora"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.articlesCount").value(1));
	}

	// --- helpers --------------------------------------------------------

	private String createArticle(String token, String title, String status) throws Exception {
		String body = mockMvc.perform(post("/api/articles")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"%s","summary":"A teaser","content":"The body.",
								 "status":"%s"}
								""".formatted(title, status)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return readJson(body).get("slug").asText();
	}

	private String update(String token, String slug, String title, String status) throws Exception {
		return mockMvc.perform(put("/api/articles/" + slug)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"%s","content":"The body.","status":"%s"}
								""".formatted(title, status)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	private MockMultipartFile pngUpload() throws IOException {
		BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(image, "png", bytes);
		return new MockMultipartFile("file", "cover.png", "image/png", bytes.toByteArray());
	}
}
