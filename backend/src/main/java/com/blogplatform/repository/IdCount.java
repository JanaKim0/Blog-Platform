package com.blogplatform.repository;

/**
 * One row of a "count per id" aggregate, so a whole page of articles can get its
 * like and comment counts in a single query instead of one query per card.
 */
public record IdCount(Long id, long count) {
}
