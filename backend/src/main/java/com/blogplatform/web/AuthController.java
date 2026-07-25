package com.blogplatform.web;

import com.blogplatform.dto.AuthResponse;
import com.blogplatform.dto.CurrentUserResponse;
import com.blogplatform.dto.LoginRequest;
import com.blogplatform.dto.RegisterRequest;
import com.blogplatform.exception.ResourceNotFoundException;
import com.blogplatform.repository.UserRepository;
import com.blogplatform.security.AppUserDetails;
import com.blogplatform.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final UserRepository users;

	public AuthController(AuthService authService, UserRepository users) {
		this.authService = authService;
		this.users = users;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	/** Who the current token belongs to - used by the app on page reload. */
	@GetMapping("/me")
	public CurrentUserResponse me(@AuthenticationPrincipal AppUserDetails principal) {
		return users.findById(principal.getId())
				.map(CurrentUserResponse::from)
				.orElseThrow(() -> ResourceNotFoundException.of("User", principal.getId()));
	}
}
