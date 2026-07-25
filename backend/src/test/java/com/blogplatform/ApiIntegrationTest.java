package com.blogplatform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Base for tests that drive the API through {@link MockMvc}. Each test runs in a
 * transaction that is rolled back afterwards, so tests never see each other's
 * rows.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class ApiIntegrationTest {

	/** The password every account created by {@link #register} gets. */
	protected static final String PASSWORD = "correct-horse";

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	/** Registers {@code <username>@example.com} and returns its bearer token. */
	protected String register(String username) throws Exception {
		return register(username, username + "@example.com");
	}

	protected String register(String username, String email) throws Exception {
		String body = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"%s","email":"%s","password":"%s"}
								""".formatted(username, email, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return readJson(body).get("token").asText();
	}

	protected JsonNode readJson(String body) {
		return objectMapper.readTree(body);
	}
}
