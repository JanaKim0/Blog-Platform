package com.blogplatform.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blogplatform.domain.User;
import com.blogplatform.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTests {

	private static final String REGISTER_BODY = """
			{"username":"nora","email":"nora@example.com","password":"correct-horse",
			 "displayName":"Nora K"}
			""";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private UserRepository users;

	@Test
	void registersAnAccountAndReturnsAToken() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.expiresAt").isNotEmpty())
				.andExpect(jsonPath("$.user.username").value("nora"))
				.andExpect(jsonPath("$.user.displayName").value("Nora K"))
				.andExpect(jsonPath("$.user.role").value("USER"))
				// the password must never travel back, in any shape
				.andExpect(jsonPath("$.user.password").doesNotExist())
				.andExpect(jsonPath("$.user.passwordHash").doesNotExist());
	}

	@Test
	void storesThePasswordOnlyAsABcryptHash() throws Exception {
		register();

		User saved = users.findByUsernameIgnoreCase("nora").orElseThrow();
		assertThat(saved.getPasswordHash()).isNotEqualTo("correct-horse");
		assertThat(saved.getPasswordHash()).startsWith("$2");
	}

	@Test
	void rejectsATakenUsernameEvenInADifferentCase() throws Exception {
		register();

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"NORA","email":"other@example.com","password":"another-pass"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void rejectsATakenEmail() throws Exception {
		register();

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"someone","email":"NORA@example.com","password":"another-pass"}
								"""))
				.andExpect(status().isConflict());
	}

	@Test
	void reportsWhichFieldsAreInvalid() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"a b","email":"not-an-email","password":"short"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.username").isNotEmpty())
				.andExpect(jsonPath("$.fieldErrors.email").isNotEmpty())
				.andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
	}

	@Test
	void signsInWithEitherTheUsernameOrTheEmail() throws Exception {
		register();

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"login":"nora","password":"correct-horse"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"login":"nora@example.com","password":"correct-horse"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.username").value("nora"));
	}

	@Test
	void rejectsAWrongPasswordWithoutSayingWhetherTheAccountExists() throws Exception {
		register();

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"login":"nora","password":"wrong-password"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid username or password"));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"login":"nobody","password":"wrong-password"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	void refusesTheCurrentUserEndpointWithoutAToken() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void refusesAGarbageToken() throws Exception {
		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not.a.token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void returnsTheCurrentUserForAValidToken() throws Exception {
		String token = register();

		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("nora"))
				.andExpect(jsonPath("$.email").value("nora@example.com"));
	}

	/** Registers the standard test account and returns its token. */
	private String register() throws Exception {
		String body = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		JsonNode json = objectMapper.readTree(body);
		return json.get("token").asText();
	}
}
