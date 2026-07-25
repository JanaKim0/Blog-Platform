package com.blogplatform.web;

import com.blogplatform.dto.ChangePasswordRequest;
import com.blogplatform.dto.CurrentUserResponse;
import com.blogplatform.dto.PageResponse;
import com.blogplatform.dto.ProfileResponse;
import com.blogplatform.dto.UpdateProfileRequest;
import com.blogplatform.dto.UserSummary;
import com.blogplatform.security.AppUserDetails;
import com.blogplatform.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	/** Search, or list everyone when no query is given. Public. */
	@GetMapping
	public PageResponse<UserSummary> search(
			@RequestParam(required = false) String query,
			@PageableDefault(size = 20, sort = "username") Pageable pageable) {
		return userService.search(query, pageable);
	}

	/** An author's public profile. Public, but richer when signed in. */
	@GetMapping("/{username}")
	public ProfileResponse profile(@PathVariable String username,
			@AuthenticationPrincipal AppUserDetails principal) {
		return userService.getProfile(username, principal == null ? null : principal.getId());
	}

	@PutMapping("/me")
	public CurrentUserResponse updateProfile(@AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody UpdateProfileRequest request) {
		return userService.updateProfile(principal.getId(), request);
	}

	@PutMapping("/me/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody ChangePasswordRequest request) {
		userService.changePassword(principal.getId(), request);
	}

	@PostMapping("/me/avatar")
	public CurrentUserResponse uploadAvatar(@AuthenticationPrincipal AppUserDetails principal,
			@RequestPart("file") MultipartFile file) {
		return userService.updateAvatar(principal.getId(), file);
	}

	@DeleteMapping("/me/avatar")
	public CurrentUserResponse removeAvatar(@AuthenticationPrincipal AppUserDetails principal) {
		return userService.removeAvatar(principal.getId());
	}
}
