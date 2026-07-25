package com.blogplatform.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blogplatform.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ArticleLikeControllerTests extends ApiIntegrationTest {

	@Test
	void likesAnArticleAndAnswersWithTheNewState() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String slug = article(author, "PUBLISHED");

		mockMvc.perform(post("/api/articles/" + slug + "/like")
						.header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likesCount").value(1))
				.andExpect(jsonPath("$.likedByMe").value(true));
	}

	@Test
	void countsEachReaderOnceHoweverManyTimesTheyTap() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String slug = article(author, "PUBLISHED");

		mockMvc.perform(post("/api/articles/" + slug + "/like")
						.header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/articles/" + slug + "/like")
						.header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likesCount").value(1));
	}

	@Test
	void addsUpLikesFromDifferentReaders() throws Exception {
		String author = register("nora");
		String first = register("mira");
		String second = register("lena");
		String slug = article(author, "PUBLISHED");

		mockMvc.perform(post("/api/articles/" + slug + "/like")
				.header("Authorization", "Bearer " + first));
		mockMvc.perform(post("/api/articles/" + slug + "/like")
						.header("Authorization", "Bearer " + second))
				.andExpect(jsonPath("$.likesCount").value(2));
	}

	@Test
	void removesTheLikeAgain() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String slug = article(author, "PUBLISHED");

		mockMvc.perform(post("/api/articles/" + slug + "/like")
				.header("Authorization", "Bearer " + reader));
		mockMvc.perform(delete("/api/articles/" + slug + "/like")
						.header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likesCount").value(0))
				.andExpect(jsonPath("$.likedByMe").value(false));
	}

	@Test
	void unlikingSomethingNeverLikedIsNotAnError() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String slug = article(author, "PUBLISHED");

		mockMvc.perform(delete("/api/articles/" + slug + "/like")
						.header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likesCount").value(0));
	}

	@Test
	void showsALikeOnlyToTheReaderWhoGaveIt() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String other = register("lena");
		String slug = article(author, "PUBLISHED");
		mockMvc.perform(post("/api/articles/" + slug + "/like")
				.header("Authorization", "Bearer " + reader));

		mockMvc.perform(get("/api/articles/" + slug).header("Authorization", "Bearer " + reader))
				.andExpect(jsonPath("$.likedByMe").value(true));
		mockMvc.perform(get("/api/articles/" + slug).header("Authorization", "Bearer " + other))
				.andExpect(jsonPath("$.likesCount").value(1))
				.andExpect(jsonPath("$.likedByMe").value(false));
		mockMvc.perform(get("/api/articles/" + slug))
				.andExpect(jsonPath("$.likedByMe").value(false));
	}

	@Test
	void marksLikedArticlesInTheFeedForTheReaderWhoLikedThem() throws Exception {
		String author = register("nora");
		String reader = register("mira");
		String liked = article(author, "PUBLISHED");
		mockMvc.perform(post("/api/articles/" + liked + "/like")
				.header("Authorization", "Bearer " + reader));

		mockMvc.perform(get("/api/articles").header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].likedByMe").value(true))
				.andExpect(jsonPath("$.content[0].likesCount").value(1));
	}

	@Test
	void letsAnAuthorLikeTheirOwnArticle() throws Exception {
		String author = register("nora");
		String slug = article(author, "PUBLISHED");

		mockMvc.perform(post("/api/articles/" + slug + "/like")
						.header("Authorization", "Bearer " + author))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.likesCount").value(1));
	}

	@Test
	void refusesALikeWithoutAToken() throws Exception {
		String author = register("nora");
		String slug = article(author, "PUBLISHED");

		mockMvc.perform(post("/api/articles/" + slug + "/like"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void refusesToLikeADraft() throws Exception {
		String author = register("nora");
		String stranger = register("mira");
		String slug = article(author, "DRAFT");

		// the author is told why
		mockMvc.perform(post("/api/articles/" + slug + "/like")
						.header("Authorization", "Bearer " + author))
				.andExpect(status().isBadRequest());
		// a stranger is not even told the draft exists
		mockMvc.perform(post("/api/articles/" + slug + "/like")
						.header("Authorization", "Bearer " + stranger))
				.andExpect(status().isNotFound());
	}

	@Test
	void answers404ForAnUnknownArticle() throws Exception {
		String token = register("nora");

		mockMvc.perform(post("/api/articles/nothing-here/like")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

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
}
