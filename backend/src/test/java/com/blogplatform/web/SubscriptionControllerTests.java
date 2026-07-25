package com.blogplatform.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blogplatform.ApiIntegrationTest;
import org.junit.jupiter.api.Test;

class SubscriptionControllerTests extends ApiIntegrationTest {

	@Test
	void followingAnAuthorShowsUpOnBothProfiles() throws Exception {
		String reader = register("mira");
		register("nora");

		mockMvc.perform(post("/api/users/nora/follow").header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("nora"))
				.andExpect(jsonPath("$.followersCount").value(1))
				.andExpect(jsonPath("$.following").value(true));

		// the follower's own profile now counts one subscription
		mockMvc.perform(get("/api/users/mira"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.followingCount").value(1))
				.andExpect(jsonPath("$.followersCount").value(0));
	}

	@Test
	void followingIsDirectedAndNotMutual() throws Exception {
		String reader = register("mira");
		String author = register("nora");

		mockMvc.perform(post("/api/users/nora/follow").header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk());

		// nora has not followed mira back
		mockMvc.perform(get("/api/users/mira").header("Authorization", "Bearer " + author))
				.andExpect(jsonPath("$.following").value(false));
	}

	@Test
	void followingTwiceLeavesASingleSubscription() throws Exception {
		String reader = register("mira");
		register("nora");

		mockMvc.perform(post("/api/users/nora/follow").header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/users/nora/follow").header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.followersCount").value(1));
	}

	@Test
	void refusesToLetSomebodyFollowThemselves() throws Exception {
		String token = register("nora");

		mockMvc.perform(post("/api/users/nora/follow").header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("You cannot follow yourself"));
	}

	@Test
	void refusesToFollowWithoutAToken() throws Exception {
		register("nora");

		mockMvc.perform(post("/api/users/nora/follow"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void answers404WhenTheAuthorDoesNotExist() throws Exception {
		String token = register("mira");

		mockMvc.perform(post("/api/users/ghost/follow").header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void unfollowingRemovesTheSubscription() throws Exception {
		String reader = register("mira");
		register("nora");

		mockMvc.perform(post("/api/users/nora/follow").header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/users/nora/follow").header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.followersCount").value(0))
				.andExpect(jsonPath("$.following").value(false));
	}

	@Test
	void unfollowingSomebodyYouNeverFollowedIsNotAnError() throws Exception {
		String reader = register("mira");
		register("nora");

		mockMvc.perform(delete("/api/users/nora/follow").header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.followersCount").value(0));
	}

	@Test
	void listsFollowersAndFollowedAuthors() throws Exception {
		String mira = register("mira");
		String lena = register("lena");
		register("nora");

		mockMvc.perform(post("/api/users/nora/follow").header("Authorization", "Bearer " + mira));
		mockMvc.perform(post("/api/users/nora/follow").header("Authorization", "Bearer " + lena));

		mockMvc.perform(get("/api/users/nora/followers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[*].username")
						.value(org.hamcrest.Matchers.containsInAnyOrder("mira", "lena")));

		mockMvc.perform(get("/api/users/mira/following"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].username").value("nora"));

		mockMvc.perform(get("/api/users/nora/following"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void tellsAnonymousVisitorsThatTheyFollowNobody() throws Exception {
		String reader = register("mira");
		register("nora");
		mockMvc.perform(post("/api/users/nora/follow").header("Authorization", "Bearer " + reader))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/users/nora"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.followersCount").value(1))
				.andExpect(jsonPath("$.following").value(false));
	}
}
