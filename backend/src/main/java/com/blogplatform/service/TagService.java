package com.blogplatform.service;

import com.blogplatform.domain.Tag;
import com.blogplatform.dto.TagResponse;
import com.blogplatform.repository.TagRepository;
import com.blogplatform.util.Slugs;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Tags are free-form: an author types them and the ones that do not exist yet
 * get created. Matching happens on the slug, so "Spring Boot", "spring boot" and
 * "spring-boot" all end up as the same tag rather than three near-duplicates.
 */
@Service
public class TagService {

	private final TagRepository tags;

	public TagService(TagRepository tags) {
		this.tags = tags;
	}

	@Transactional(readOnly = true)
	public List<TagResponse> list() {
		return tags.findAllByOrderByNameAsc().stream().map(TagResponse::from).toList();
	}

	/** Finds or creates the tags for {@code names}, ignoring blanks and repeats. */
	@Transactional
	public Set<Tag> resolve(List<String> names) {
		if (names == null || names.isEmpty()) {
			return Set.of();
		}

		// Keyed by slug, so two spellings of the same tag collapse into one.
		Map<String, String> wantedBySlug = new LinkedHashMap<>();
		for (String name : names) {
			if (!StringUtils.hasText(name)) {
				continue;
			}
			String trimmed = name.trim();
			String slug = Slugs.slugify(trimmed, "", 60);
			if (!slug.isEmpty()) {
				wantedBySlug.putIfAbsent(slug, trimmed);
			}
		}
		if (wantedBySlug.isEmpty()) {
			return Set.of();
		}

		Set<Tag> resolved = new LinkedHashSet<>(tags.findBySlugIn(wantedBySlug.keySet()));
		resolved.forEach(tag -> wantedBySlug.remove(tag.getSlug()));

		wantedBySlug.forEach((slug, name) -> resolved.add(create(slug, name)));
		return resolved;
	}

	private Tag create(String slug, String name) {
		Tag tag = new Tag();
		tag.setSlug(slug);
		tag.setName(name.length() > 50 ? name.substring(0, 50) : name);
		try {
			return tags.saveAndFlush(tag);
		}
		catch (DataIntegrityViolationException ex) {
			// Another request created the same tag a moment ago; use theirs.
			return tags.findBySlug(slug).orElseThrow(() -> ex);
		}
	}
}
