package com.blogplatform.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/**
 * The single error shape the API returns, so the Angular app only ever has to
 * parse one thing. {@code fieldErrors} is present only for validation failures.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(Instant timestamp, int status, String error, String message,
		Map<String, String> fieldErrors) {

	public static ApiError of(int status, String error, String message) {
		return new ApiError(Instant.now(), status, error, message, null);
	}

	public static ApiError of(int status, String error, String message, Map<String, String> fieldErrors) {
		return new ApiError(Instant.now(), status, error, message, fieldErrors);
	}
}
