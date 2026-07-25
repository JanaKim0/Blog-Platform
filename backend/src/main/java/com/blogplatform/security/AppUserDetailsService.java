package com.blogplatform.security;

import com.blogplatform.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads accounts for Spring Security. Accepts either the username or the email
 * address, so people can sign in with whichever they remember.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

	private final UserRepository users;

	public AppUserDetailsService(UserRepository users) {
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
		return users.findByUsernameIgnoreCase(usernameOrEmail)
				.or(() -> users.findByEmailIgnoreCase(usernameOrEmail))
				.map(AppUserDetails::new)
				.orElseThrow(() -> new UsernameNotFoundException("No account for " + usernameOrEmail));
	}
}
