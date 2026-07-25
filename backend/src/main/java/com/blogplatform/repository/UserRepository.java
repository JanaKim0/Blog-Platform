package com.blogplatform.repository;

import com.blogplatform.domain.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsernameIgnoreCase(String username);

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByUsernameIgnoreCase(String username);

	boolean existsByEmailIgnoreCase(String email);

	/** Free-text user search over the login name, display name and bio. */
	@Query("""
			select u from User u
			where u.enabled = true
			  and (lower(u.username) like lower(concat('%', :query, '%'))
			    or lower(coalesce(u.displayName, '')) like lower(concat('%', :query, '%')))
			""")
	Page<User> search(@Param("query") String query, Pageable pageable);
}
