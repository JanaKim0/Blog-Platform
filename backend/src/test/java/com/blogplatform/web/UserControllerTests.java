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
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class UserControllerTests extends ApiIntegrationTest {

	@Value("${app.storage.location}")
	private String storageLocation;

	// --- public profile & search ----------------------------------------

	@Test
	void showsAPublicProfileWithoutTheEmailAddress() throws Exception {
		register("nora", "nora@example.com");

		mockMvc.perform(get("/api/users/nora"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("nora"))
				.andExpect(jsonPath("$.articlesCount").value(0))
				.andExpect(jsonPath("$.followersCount").value(0))
				.andExpect(jsonPath("$.email").doesNotExist());
	}

	@Test
	void answers404ForAnUnknownUser() throws Exception {
		mockMvc.perform(get("/api/users/nobody"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void findsUsersByPartOfTheirName() throws Exception {
		register("nora", "nora@example.com");
		register("mira", "mira@example.com");

		mockMvc.perform(get("/api/users").param("query", "nor"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].username").value("nora"))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void listsEveryoneWhenTheSearchQueryIsEmpty() throws Exception {
		register("nora", "nora@example.com");
		register("mira", "mira@example.com");

		mockMvc.perform(get("/api/users"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.size").value(20));
	}

	@Test
	void capsThePageSizeAClientMayAskFor() throws Exception {
		register("nora", "nora@example.com");

		mockMvc.perform(get("/api/users").param("size", "5000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size").value(50));
	}

	// --- editing one's own profile --------------------------------------

	@Test
	void refusesProfileEditsWithoutAToken() throws Exception {
		mockMvc.perform(put("/api/users/me")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"Hacker","email":"hacker@example.com"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void updatesTheDisplayNameBioAndEmail() throws Exception {
		String token = register("nora", "nora@example.com");

		mockMvc.perform(put("/api/users/me").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"Nora K","bio":"I write about Java.",
								 "email":"nora.new@example.com"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Nora K"))
				.andExpect(jsonPath("$.bio").value("I write about Java."))
				.andExpect(jsonPath("$.email").value("nora.new@example.com"));
	}

	@Test
	void refusesAnEmailThatBelongsToSomebodyElse() throws Exception {
		String token = register("nora", "nora@example.com");
		register("mira", "mira@example.com");

		mockMvc.perform(put("/api/users/me").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"mira@example.com"}
								"""))
				.andExpect(status().isConflict());
	}

	@Test
	void acceptsKeepingOnesOwnEmailUnchanged() throws Exception {
		String token = register("nora", "nora@example.com");

		mockMvc.perform(put("/api/users/me").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"Nora","email":"nora@example.com"}
								"""))
				.andExpect(status().isOk());
	}

	// --- password -------------------------------------------------------

	@Test
	void refusesAPasswordChangeWithTheWrongCurrentPassword() throws Exception {
		String token = register("nora", "nora@example.com");

		mockMvc.perform(put("/api/users/me/password").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"currentPassword":"not-it","newPassword":"brand-new-pass"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("The current password is incorrect"));
	}

	@Test
	void refusesReusingTheSamePassword() throws Exception {
		String token = register("nora", "nora@example.com");

		mockMvc.perform(put("/api/users/me/password").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"currentPassword":"correct-horse","newPassword":"correct-horse"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void changesThePasswordAndThenAcceptsOnlyTheNewOne() throws Exception {
		String token = register("nora", "nora@example.com");

		mockMvc.perform(put("/api/users/me/password").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"currentPassword":"correct-horse","newPassword":"brand-new-pass"}
								"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"login":"nora","password":"brand-new-pass"}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"login":"nora","password":"correct-horse"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	// --- avatar ---------------------------------------------------------

	@Test
	void refusesAnAvatarUploadWithoutAToken() throws Exception {
		mockMvc.perform(multipart("/api/users/me/avatar").file(pngUpload(100, 100)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void storesAnAvatarAndScalesItDownToTheAllowedSize() throws Exception {
		String token = register("nora", "nora@example.com");

		String body = mockMvc.perform(multipart("/api/users/me/avatar")
						.file(pngUpload(1200, 900))
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.avatarUrl").isNotEmpty())
				.andReturn().getResponse().getContentAsString();

		String avatarUrl = objectMapper.readTree(body).get("avatarUrl").asText();
		assertThat(avatarUrl).startsWith("/uploads/");

		BufferedImage stored = ImageIO.read(storedFile(avatarUrl).toFile());
		assertThat(stored).isNotNull();
		// 512 is ImageKind.AVATAR's limit; 1200x900 must come out as 512x384.
		assertThat(stored.getWidth()).isEqualTo(512);
		assertThat(stored.getHeight()).isEqualTo(384);
	}

	@Test
	void leavesSmallAvatarsAtTheirOriginalSize() throws Exception {
		String token = register("nora", "nora@example.com");

		String body = mockMvc.perform(multipart("/api/users/me/avatar")
						.file(pngUpload(120, 120))
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		BufferedImage stored = ImageIO.read(
				storedFile(objectMapper.readTree(body).get("avatarUrl").asText()).toFile());
		assertThat(stored.getWidth()).isEqualTo(120);
	}

	@Test
	void rejectsAFileThatOnlyClaimsToBeAnImage() throws Exception {
		String token = register("nora", "nora@example.com");

		MockMultipartFile notAnImage = new MockMultipartFile("file", "payload.png",
				"image/png", "<html>definitely not a picture</html>".getBytes());

		mockMvc.perform(multipart("/api/users/me/avatar").file(notAnImage)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("The uploaded file is not a readable image"));
	}

	@Test
	void rejectsAnUnsupportedImageFormat() throws Exception {
		String token = register("nora", "nora@example.com");

		MockMultipartFile svg = new MockMultipartFile("file", "logo.svg", "image/svg+xml",
				"<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes());

		mockMvc.perform(multipart("/api/users/me/avatar").file(svg)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("Only JPEG, PNG and GIF images are supported"));
	}

	@Test
	void deletesTheOldFileWhenTheAvatarIsReplacedOrRemoved() throws Exception {
		String token = register("nora", "nora@example.com");

		String first = uploadAvatar(token, 200, 200);
		Path firstFile = storedFile(first);
		assertThat(firstFile).exists();

		String second = uploadAvatar(token, 200, 200);
		assertThat(second).isNotEqualTo(first);
		assertThat(firstFile).doesNotExist();

		mockMvc.perform(delete("/api/users/me/avatar").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.avatarUrl").doesNotExist());
		assertThat(storedFile(second)).doesNotExist();
	}

	// --- helpers --------------------------------------------------------

	private String uploadAvatar(String token, int width, int height) throws Exception {
		String body = mockMvc.perform(multipart("/api/users/me/avatar")
						.file(pngUpload(width, height))
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body).get("avatarUrl").asText();
	}

	/** Maps a public {@code /uploads/...} URL back to the file on disk. */
	private Path storedFile(String publicUrl) {
		Path root = Paths.get(storageLocation).toAbsolutePath().normalize();
		return root.resolve(publicUrl.substring("/uploads/".length()));
	}

	private MockMultipartFile pngUpload(int width, int height) throws IOException {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(image, "png", bytes);
		return new MockMultipartFile("file", "avatar.png", "image/png", bytes.toByteArray());
	}
}
