package com.blogplatform.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blogplatform.ApiIntegrationTest;
import com.blogplatform.domain.Role;
import com.blogplatform.domain.User;
import com.blogplatform.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class CategoryControllerTests extends ApiIntegrationTest {

	@Autowired
	private UserRepository users;

	@Test
	void listsTheSeededCategoriesInAlphabeticalOrder() throws Exception {
		mockMvc.perform(get("/api/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(7))
				.andExpect(jsonPath("$[0].name").value("Career"))
				.andExpect(jsonPath("$[0].slug").value("career"))
				.andExpect(jsonPath("$[6].name").value("Travel"));
	}

	@Test
	void refusesCategoryCreationForAnonymousCallers() throws Exception {
		mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Sneaky"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void refusesCategoryCreationForOrdinaryUsers() throws Exception {
		String token = register("nora");

		mockMvc.perform(post("/api/categories").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"My Own Category"}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void letsAnAdministratorAddACategory() throws Exception {
		String token = register("nora");
		promoteToAdmin("nora");

		mockMvc.perform(post("/api/categories").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Machine Learning","description":"Models and data"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Machine Learning"))
				.andExpect(jsonPath("$.slug").value("machine-learning"));
	}

	@Test
	void rejectsACategoryThatAlreadyExists() throws Exception {
		String token = register("nora");
		promoteToAdmin("nora");

		mockMvc.perform(post("/api/categories").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"programming"}
								"""))
				.andExpect(status().isConflict());
	}

	@Test
	void deletingACategoryLeavesItsArticlesUncategorised() throws Exception {
		String token = register("nora");
		mockMvc.perform(post("/api/articles").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"On Spring","content":"Body.","status":"PUBLISHED",
								 "categorySlug":"programming"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.category.slug").value("programming"));

		promoteToAdmin("nora");
		mockMvc.perform(delete("/api/categories/programming")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/categories"))
				.andExpect(jsonPath("$.length()").value(6));
		// the article survives, it simply has no category any more
		mockMvc.perform(get("/api/articles/on-spring"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.category").doesNotExist());
	}

	/**
	 * Authorities are read from the database on every request, so promoting a
	 * user takes effect on their existing token without a new sign-in.
	 */
	private void promoteToAdmin(String username) {
		User user = users.findByUsernameIgnoreCase(username).orElseThrow();
		user.setRole(Role.ADMIN);
		users.saveAndFlush(user);
	}
}
