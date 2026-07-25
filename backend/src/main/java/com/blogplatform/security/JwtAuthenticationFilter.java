package com.blogplatform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Turns a {@code Authorization: Bearer <token>} header into an authenticated
 * request. A missing or invalid token is not an error here - the request simply
 * stays anonymous, and the authorization rules decide whether that is allowed.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String HEADER = "Authorization";
	private static final String PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final AppUserDetailsService userDetailsService;

	public JwtAuthenticationFilter(JwtService jwtService, AppUserDetailsService userDetailsService) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String token = bearerToken(request);
		if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			jwtService.readUsername(token).ifPresent(username -> authenticate(username, request));
		}
		filterChain.doFilter(request, response);
	}

	private void authenticate(String username, HttpServletRequest request) {
		try {
			UserDetails user = userDetailsService.loadUserByUsername(username);
			if (!user.isEnabled()) {
				return;
			}
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		catch (UsernameNotFoundException ex) {
			// The token names an account that no longer exists - stay anonymous.
			SecurityContextHolder.clearContext();
		}
	}

	private String bearerToken(HttpServletRequest request) {
		String header = request.getHeader(HEADER);
		if (header == null || !header.startsWith(PREFIX)) {
			return null;
		}
		String token = header.substring(PREFIX.length()).trim();
		return token.isEmpty() ? null : token;
	}
}
