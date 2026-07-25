package com.blogplatform.security;

import com.blogplatform.config.JwtProperties;
import com.blogplatform.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Issues and reads the signed tokens that stand in for a login session. */
@Service
public class JwtService {

	private final SecretKey key;
	private final long expirationMs;

	public JwtService(JwtProperties properties) {
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
		this.expirationMs = properties.expirationMs();
	}

	/** A token identifying the user, valid for {@code app.jwt.expiration-ms}. */
	public String createToken(User user) {
		Instant issuedAt = Instant.now();
		return Jwts.builder()
				.subject(user.getUsername())
				.claim("uid", user.getId())
				.claim("role", user.getRole().name())
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(issuedAt.plusMillis(expirationMs)))
				.signWith(key)
				.compact();
	}

	/** When a token issued right now would expire. */
	public Instant expiresAt() {
		return Instant.now().plusMillis(expirationMs);
	}

	/**
	 * The username inside the token, or empty if the token is malformed, expired
	 * or signed with a different key.
	 */
	public Optional<String> readUsername(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(key)
					.build()
					.parseSignedClaims(token)
					.getPayload();
			return Optional.ofNullable(claims.getSubject());
		}
		catch (JwtException | IllegalArgumentException ex) {
			return Optional.empty();
		}
	}
}
