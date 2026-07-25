package com.blogplatform.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blogplatform.ApiIntegrationTest;
import com.blogplatform.domain.Role;
import com.blogplatform.domain.User;
import com.blogplatform.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class CommentControllerTests extends ApiIntegrationTest {

	@Autowired
	private UserRepository users;

	// --- posting --------------------------------------------------------

	@Test
	void addsACommentToAPublishedArticle() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String slug = article(author, "PUBLISHED");

		mockMvc.perform(post("/api/articles/" + slug + "/comments")
						.header("Authorization", "Bearer " + reader)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"  Nice article!  "}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.content").value("Nice article!"))
				.andExpect(jsonPath("$.author.username").value("mira"))
				.andExpect(jsonPath("$.edited").value(false))
				.andExpect(jsonPath("$.createdAt").isNotEmpty());
	}

	@Test
	void showsTheCommentCountOnTheArticle() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String slug = article(author, "PUBLISHED");
		comment(reader, slug, "First");
		comment(author, slug, "Thanks!");

		mockMvc.perform(get("/api/articles/" + slug))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.commentsCount").value(2));
	}

	@Test
	void refusesACommentWithoutAToken() throws Exception {
		String author = register("nora");
		String slug = article(author, "PUBLISHED");

		mockMvc.perform(post("/api/articles/" + slug + "/comments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"Anonymous"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void refusesAnEmptyComment() throws Exception {
		String author = register("nora");
		String slug = article(author, "PUBLISHED");

		mockMvc.perform(post("/api/articles/" + slug + "/comments")
						.header("Authorization", "Bearer " + author)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"   "}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.content").isNotEmpty());
	}

	@Test
	void hidesTheCommentsOfADraftFromStrangersButNotFromItsAuthor() throws Exception {
		String author = register("nora");
		String stranger = register("mira");
		String slug = article(author, "DRAFT");

		mockMvc.perform(get("/api/articles/" + slug + "/comments"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/articles/" + slug + "/comments")
						.header("Authorization", "Bearer " + stranger))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/articles/" + slug + "/comments")
						.header("Authorization", "Bearer " + author))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void refusesToCommentOnADraftEvenForItsOwnAuthor() throws Exception {
		String author = register("nora");
		String slug = article(author, "DRAFT");

		mockMvc.perform(post("/api/articles/" + slug + "/comments")
						.header("Authorization", "Bearer " + author)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"Note to self"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("This article is still a draft"));
	}

	@Test
	void answers404WhenTheArticleDoesNotExist() throws Exception {
		String token = register("nora");

		mockMvc.perform(post("/api/articles/nothing-here/comments")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"Hello?"}
								"""))
				.andExpect(status().isNotFound());
	}

	// --- listing --------------------------------------------------------

	@Test
	void listsCommentsOldestFirstAndPaginatesThem() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String slug = article(author, "PUBLISHED");
		comment(reader, slug, "One");
		comment(reader, slug, "Two");
		comment(reader, slug, "Three");

		mockMvc.perform(get("/api/articles/" + slug + "/comments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.content[0].content").value("One"))
				.andExpect(jsonPath("$.content[2].content").value("Three"));

		mockMvc.perform(get("/api/articles/" + slug + "/comments").param("size", "2"))
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.totalPages").value(2));
	}

	// --- editing --------------------------------------------------------

	@Test
	void letsTheAuthorOfACommentEditItAndMarksItEdited() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String slug = article(author, "PUBLISHED");
		long commentId = comment(reader, slug, "Frist");

		mockMvc.perform(put("/api/comments/" + commentId)
						.header("Authorization", "Bearer " + reader)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"First"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("First"))
				.andExpect(jsonPath("$.edited").value(true));
	}

	@Test
	void refusesToLetTheArticleAuthorRewriteSomebodyElsesComment() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String slug = article(author, "PUBLISHED");
		long commentId = comment(reader, slug, "I disagree");

		mockMvc.perform(put("/api/comments/" + commentId)
						.header("Authorization", "Bearer " + author)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"I agree completely"}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void refusesToLetAnAdministratorRewriteAComment() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String slug = article(author, "PUBLISHED");
		long commentId = comment(reader, slug, "I disagree");
		promoteToAdmin("nora");

		mockMvc.perform(put("/api/comments/" + commentId)
						.header("Authorization", "Bearer " + author)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"Rewritten by an admin"}
								"""))
				.andExpect(status().isForbidden());
	}

	// --- deleting -------------------------------------------------------

	@Test
	void letsTheAuthorOfACommentDeleteIt() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String slug = article(author, "PUBLISHED");
		long commentId = comment(reader, slug, "Never mind");

		mockMvc.perform(delete("/api/comments/" + commentId)
						.header("Authorization", "Bearer " + reader))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/articles/" + slug + "/comments"))
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void letsTheArticleAuthorModerateCommentsOnTheirOwnArticle() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String slug = article(author, "PUBLISHED");
		long commentId = comment(reader, slug, "Spam spam spam");

		mockMvc.perform(delete("/api/comments/" + commentId)
						.header("Authorization", "Bearer " + author))
				.andExpect(status().isNoContent());
	}

	@Test
	void letsAnAdministratorDeleteAnyComment() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String moderator = register("lena");
		String slug = article(author, "PUBLISHED");
		long commentId = comment(reader, slug, "Spam");
		promoteToAdmin("lena");

		mockMvc.perform(delete("/api/comments/" + commentId)
						.header("Authorization", "Bearer " + moderator))
				.andExpect(status().isNoContent());
	}

	@Test
	void refusesDeletionByAnUnrelatedReader() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String bystander = register("lena");
		String slug = article(author, "PUBLISHED");
		long commentId = comment(reader, slug, "My comment");

		mockMvc.perform(delete("/api/comments/" + commentId)
						.header("Authorization", "Bearer " + bystander))
				.andExpect(status().isForbidden());
	}

	@Test
	void answers404ForAnUnknownComment() throws Exception {
		String token = register("nora");

		mockMvc.perform(delete("/api/comments/99999").header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void removesTheCommentsOfADeletedArticle() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String slug = article(author, "PUBLISHED");
		comment(reader, slug, "Will vanish with the article");

		mockMvc.perform(delete("/api/articles/" + slug)
						.header("Authorization", "Bearer " + author))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/articles/" + slug + "/comments"))
				.andExpect(status().isNotFound());
	}

	// --- helpers --------------------------------------------------------

	private String article(String token, String status) throws Exception {
		String body = mockMvc.perform(post("/api/articles")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"An Article","content":"The body.","status":"%s"}
								""".formatted(status)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return readJson(body).get("slug").asText();
	}

	/** Posts a comment and returns its id. */
	private long comment(String token, String slug, String text) throws Exception {
		String body = mockMvc.perform(post("/api/articles/" + slug + "/comments")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"%s"}
								""".formatted(text)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return readJson(body).get("id").asLong();
	}

	private void promoteToAdmin(String username) {
		User user = users.findByUsernameIgnoreCase(username).orElseThrow();
		user.setRole(Role.ADMIN);
		users.saveAndFlush(user);
	}
}
