package com.blogplatform.security;

import com.blogplatform.domain.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security's view of a logged-in account. Controllers receive this via
 * {@code @AuthenticationPrincipal}, which is why it also exposes the user id -
 * that saves a database lookup on almost every authenticated request.
 */
public class AppUserDetails implements UserDetails {

	private final Long id;
	private final String username;
	private final String passwordHash;
	private final boolean enabled;
	private final List<GrantedAuthority> authorities;

	public AppUserDetails(User user) {
		this.id = user.getId();
		this.username = user.getUsername();
		this.passwordHash = user.getPasswordHash();
		this.enabled = user.isEnabled();
		this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
	}

	public Long getId() {
		return id;
	}

	/** Administrators may edit and remove other people's articles. */
	public boolean isAdmin() {
		return authorities.stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
