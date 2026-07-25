package com.blogplatform.exception;

/** Thrown when a requested article, user, comment or category does not exist. */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public static ResourceNotFoundException of(String what, Object id) {
		return new ResourceNotFoundException(what + " " + id + " was not found");
	}
}
