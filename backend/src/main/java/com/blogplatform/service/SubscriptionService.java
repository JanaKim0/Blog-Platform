package com.blogplatform.service;

import com.blogplatform.domain.Subscription;
import com.blogplatform.domain.User;
import com.blogplatform.dto.PageResponse;
import com.blogplatform.dto.ProfileResponse;
import com.blogplatform.dto.UserSummary;
import com.blogplatform.exception.BadRequestException;
import com.blogplatform.exception.ResourceNotFoundException;
import com.blogplatform.repository.SubscriptionRepository;
import com.blogplatform.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Following and unfollowing authors, and listing who follows whom. */
@Service
public class SubscriptionService {

	private final SubscriptionRepository subscriptions;
	private final UserRepository users;
	private final UserService userService;

	public SubscriptionService(SubscriptionRepository subscriptions, UserRepository users,
			UserService userService) {
		this.subscriptions = subscriptions;
		this.users = users;
		this.userService = userService;
	}

	/**
	 * Follows {@code username}. Following someone twice is not an error - the
	 * button in the UI is a state, not a counter - so this is idempotent.
	 */
	@Transactional
	public ProfileResponse follow(Long followerId, String username) {
		User author = requireUser(username);
		if (author.getId().equals(followerId)) {
			throw new BadRequestException("You cannot follow yourself");
		}

		if (!subscriptions.existsByFollowerIdAndAuthorId(followerId, author.getId())) {
			Subscription subscription = new Subscription();
			subscription.setFollower(users.getReferenceById(followerId));
			subscription.setAuthor(author);
			try {
				subscriptions.saveAndFlush(subscription);
			}
			catch (DataIntegrityViolationException ex) {
				// Two clicks arriving at once: the unique constraint caught the
				// second one, and the end state is the one that was asked for.
			}
		}
		return userService.getProfile(username, followerId);
	}

	@Transactional
	public ProfileResponse unfollow(Long followerId, String username) {
		User author = requireUser(username);
		subscriptions.findByFollowerIdAndAuthorId(followerId, author.getId())
				.ifPresent(subscriptions::delete);
		return userService.getProfile(username, followerId);
	}

	/** The people who follow this author. */
	@Transactional(readOnly = true)
	public PageResponse<UserSummary> followers(String username, Pageable pageable) {
		User author = requireUser(username);
		return PageResponse.from(subscriptions.findByAuthorId(author.getId(), pageable),
				subscription -> UserSummary.from(subscription.getFollower()));
	}

	/** The authors this user follows. */
	@Transactional(readOnly = true)
	public PageResponse<UserSummary> following(String username, Pageable pageable) {
		User user = requireUser(username);
		return PageResponse.from(subscriptions.findByFollowerId(user.getId(), pageable),
				subscription -> UserSummary.from(subscription.getAuthor()));
	}

	private User requireUser(String username) {
		return users.findByUsernameIgnoreCase(username)
				.orElseThrow(() -> new ResourceNotFoundException("No user named " + username));
	}
}
