package com.blogplatform.service;

import com.blogplatform.domain.ArticleStatus;
import com.blogplatform.domain.User;
import com.blogplatform.dto.ChangePasswordRequest;
import com.blogplatform.dto.CurrentUserResponse;
import com.blogplatform.dto.PageResponse;
import com.blogplatform.dto.ProfileResponse;
import com.blogplatform.dto.UpdateProfileRequest;
import com.blogplatform.dto.UserSummary;
import com.blogplatform.exception.BadRequestException;
import com.blogplatform.exception.ConflictException;
import com.blogplatform.exception.ResourceNotFoundException;
import com.blogplatform.repository.ArticleRepository;
import com.blogplatform.repository.SubscriptionRepository;
import com.blogplatform.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** Reading and editing profiles, and the avatar that goes with them. */
@Service
public class UserService {

	private final UserRepository users;
	private final ArticleRepository articles;
	private final SubscriptionRepository subscriptions;
	private final PasswordEncoder passwordEncoder;
	private final ImageStorageService imageStorage;

	public UserService(UserRepository users, ArticleRepository articles,
			SubscriptionRepository subscriptions, PasswordEncoder passwordEncoder,
			ImageStorageService imageStorage) {
		this.users = users;
		this.articles = articles;
		this.subscriptions = subscriptions;
		this.passwordEncoder = passwordEncoder;
		this.imageStorage = imageStorage;
	}

	/**
	 * @param currentUserId the signed-in visitor, or {@code null} for anonymous
	 *                      ones - it only decides the {@code following} flag
	 */
	@Transactional(readOnly = true)
	public ProfileResponse getProfile(String username, Long currentUserId) {
		User user = users.findByUsernameIgnoreCase(username)
				.orElseThrow(() -> new ResourceNotFoundException("No user named " + username));
		boolean following = currentUserId != null
				&& subscriptions.existsByFollowerIdAndAuthorId(currentUserId, user.getId());
		return ProfileResponse.of(user,
				articles.countByAuthorIdAndStatus(user.getId(), ArticleStatus.PUBLISHED),
				subscriptions.countByAuthorId(user.getId()),
				subscriptions.countByFollowerId(user.getId()),
				following);
	}

	/** An empty query lists everyone, which is what an open search page shows. */
	@Transactional(readOnly = true)
	public PageResponse<UserSummary> search(String query, Pageable pageable) {
		Page<User> page = StringUtils.hasText(query)
				? users.search(query.trim(), pageable)
				: users.findAll(pageable);
		return PageResponse.from(page, UserSummary::from);
	}

	@Transactional
	public CurrentUserResponse updateProfile(Long userId, UpdateProfileRequest request) {
		User user = requireUser(userId);
		String email = request.email().trim();

		if (!email.equalsIgnoreCase(user.getEmail()) && users.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("An account with this email already exists");
		}

		user.setEmail(email);
		user.setDisplayName(blankToNull(request.displayName()));
		user.setBio(blankToNull(request.bio()));
		return CurrentUserResponse.from(users.save(user));
	}

	@Transactional
	public void changePassword(Long userId, ChangePasswordRequest request) {
		User user = requireUser(userId);

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new BadRequestException("The current password is incorrect");
		}
		if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
			throw new BadRequestException("The new password must differ from the current one");
		}

		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		users.save(user);
	}

	@Transactional
	public CurrentUserResponse updateAvatar(Long userId, MultipartFile file) {
		User user = requireUser(userId);
		String previous = user.getAvatarUrl();

		user.setAvatarUrl(imageStorage.store(file, ImageKind.AVATAR));
		CurrentUserResponse response = CurrentUserResponse.from(users.save(user));

		// Only once the new avatar is safely stored is the old file thrown away.
		imageStorage.delete(previous);
		return response;
	}

	@Transactional
	public CurrentUserResponse removeAvatar(Long userId) {
		User user = requireUser(userId);
		String previous = user.getAvatarUrl();

		user.setAvatarUrl(null);
		CurrentUserResponse response = CurrentUserResponse.from(users.save(user));

		imageStorage.delete(previous);
		return response;
	}

	private User requireUser(Long userId) {
		return users.findById(userId)
				.orElseThrow(() -> ResourceNotFoundException.of("User", userId));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
