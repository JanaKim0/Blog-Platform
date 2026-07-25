package com.blogplatform.web;

import com.blogplatform.dto.PageResponse;
import com.blogplatform.dto.ProfileResponse;
import com.blogplatform.dto.UserSummary;
import com.blogplatform.security.AppUserDetails;
import com.blogplatform.service.SubscriptionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Following authors. Reading the lists is public; following is not. */
@RestController
@RequestMapping("/api/users/{username}")
public class SubscriptionController {

	private final SubscriptionService subscriptionService;

	public SubscriptionController(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}

	@PostMapping("/follow")
	public ProfileResponse follow(@PathVariable String username,
			@AuthenticationPrincipal AppUserDetails principal) {
		return subscriptionService.follow(principal.getId(), username);
	}

	@DeleteMapping("/follow")
	public ProfileResponse unfollow(@PathVariable String username,
			@AuthenticationPrincipal AppUserDetails principal) {
		return subscriptionService.unfollow(principal.getId(), username);
	}

	@GetMapping("/followers")
	public PageResponse<UserSummary> followers(@PathVariable String username,
			@PageableDefault(size = 20, sort = "createdAt",
					direction = Sort.Direction.DESC) Pageable pageable) {
		return subscriptionService.followers(username, pageable);
	}

	@GetMapping("/following")
	public PageResponse<UserSummary> following(@PathVariable String username,
			@PageableDefault(size = 20, sort = "createdAt",
					direction = Sort.Direction.DESC) Pageable pageable) {
		return subscriptionService.following(username, pageable);
	}
}
