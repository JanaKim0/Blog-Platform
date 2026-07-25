package com.blogplatform.dto;

/**
 * How the article list is ordered. Popularity sorts fall back to the
 * publication date for ties, so the order is always deterministic.
 */
public enum ArticleSort {

	/** Newest publication first. */
	RECENT,
	/** Most liked first. */
	LIKES,
	/** Most discussed first. */
	COMMENTS
}
