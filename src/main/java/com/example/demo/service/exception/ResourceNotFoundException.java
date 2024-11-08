package com.example.demo.service.exception;

public class ResourceNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 3059331149676995805L;

	public ResourceNotFoundException(Object id) {
		super("Resource not found. id " + id);
	}

}
