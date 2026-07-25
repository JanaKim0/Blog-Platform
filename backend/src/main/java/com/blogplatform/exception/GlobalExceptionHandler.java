package com.blogplatform.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Translates exceptions into the {@link ApiError} shape. */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
		return build(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
		// Deliberately vague: telling callers whether the account exists would
		// let them enumerate usernames.
		return build(HttpStatus.UNAUTHORIZED, "Invalid username or password");
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
		return build(HttpStatus.FORBIDDEN, "You are not allowed to do that");
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiError> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
		return build(HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded file is too large");
	}

	/** Bean-validation failures on {@code @Valid} request bodies. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		HttpStatus status = HttpStatus.BAD_REQUEST;
		return ResponseEntity.status(status).body(ApiError.of(status.value(),
				status.getReasonPhrase(), "Some fields are invalid", fieldErrors));
	}

	private ResponseEntity<ApiError> build(HttpStatus status, String message) {
		return ResponseEntity.status(status)
				.body(ApiError.of(status.value(), status.getReasonPhrase(), message));
	}
}
