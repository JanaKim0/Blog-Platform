package com.blogplatform.dto;

/**
 * The filters the article list accepts. Every field is optional; a {@code null}
 * simply means "do not narrow by this".
 *
 * @param query    free text matched against the title, summary and body
 * @param category a category slug
 * @param tag      a tag slug
 * @param author   an author's username
 */
public record ArticleSearchRequest(String query, String category, String tag, String author,
		ArticleSort sortBy) {

	public ArticleSort sortOrDefault() {
		return sortBy == null ? ArticleSort.RECENT : sortBy;
	}
}
