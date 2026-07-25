package com.blogplatform.exception;

/** Thrown when a request clashes with existing data, e.g. a taken username. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
