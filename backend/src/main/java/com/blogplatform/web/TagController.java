package com.blogplatform.web;

import com.blogplatform.dto.TagResponse;
import com.blogplatform.service.TagService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tags are not created through this endpoint - they appear when an author uses
 * them on an article. This is the list for the editor's suggestions and for the
 * filter sidebar.
 */
@RestController
@RequestMapping("/api/tags")
public class TagController {

	private final TagService tagService;

	public TagController(TagService tagService) {
		this.tagService = tagService;
	}

	@GetMapping
	public List<TagResponse> list() {
		return tagService.list();
	}
}
