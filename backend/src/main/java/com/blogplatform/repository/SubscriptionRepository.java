package com.blogplatform.repository;

import com.blogplatform.domain.Subscription;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	Optional<Subscription> findByFollowerIdAndAuthorId(Long followerId, Long authorId);

	boolean existsByFollowerIdAndAuthorId(Long followerId, Long authorId);

	/** How many people follow this author. */
	long countByAuthorId(Long authorId);

	/** How many authors this user follows. */
	long countByFollowerId(Long followerId);

	@EntityGraph(attributePaths = {"author"})
	Page<Subscription> findByFollowerId(Long followerId, Pageable pageable);

	@EntityGraph(attributePaths = {"follower"})
	Page<Subscription> findByAuthorId(Long authorId, Pageable pageable);

	/** Authors this user follows - the input of the personal feed query. */
	@Query("select s.author.id from Subscription s where s.follower.id = :followerId")
	List<Long> findFollowedAuthorIds(@Param("followerId") Long followerId);

	/** Which of {@code authorIds} the user already follows, in one query. */
	@Query("select s.author.id from Subscription s where s.follower.id = :followerId and s.author.id in :authorIds")
	List<Long> findFollowedAuthorIdsAmong(@Param("followerId") Long followerId,
			@Param("authorIds") Collection<Long> authorIds);
}
