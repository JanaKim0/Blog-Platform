package com.blogplatform.service;

import com.blogplatform.domain.Role;
import com.blogplatform.domain.User;
import com.blogplatform.dto.AuthResponse;
import com.blogplatform.dto.CurrentUserResponse;
import com.blogplatform.dto.LoginRequest;
import com.blogplatform.dto.RegisterRequest;
import com.blogplatform.exception.ConflictException;
import com.blogplatform.exception.ResourceNotFoundException;
import com.blogplatform.repository.UserRepository;
import com.blogplatform.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration and sign-in. */
@Service
public class AuthService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager, JwtService jwtService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String username = request.username().trim();
		String email = request.email().trim();

		if (users.existsByUsernameIgnoreCase(username)) {
			throw new ConflictException("Username \"" + username + "\" is already taken");
		}
		if (users.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("An account with this email already exists");
		}

		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setDisplayName(blankToNull(request.displayName()));
		user.setRole(Role.USER);
		user.setEnabled(true);

		return tokenFor(users.save(user));
	}

	/**
	 * Verifies the credentials through Spring Security, so a wrong password
	 * raises {@code BadCredentialsException} and is answered with 401.
	 */
	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String login = request.login().trim();
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(login, request.password()));

		User user = users.findByUsernameIgnoreCase(login)
				.or(() -> users.findByEmailIgnoreCase(login))
				.orElseThrow(() -> new ResourceNotFoundException("Account " + login + " was not found"));
		return tokenFor(user);
	}

	private AuthResponse tokenFor(User user) {
		return new AuthResponse(jwtService.createToken(user), jwtService.expiresAt(),
				CurrentUserResponse.from(user));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
