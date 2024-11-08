package com.example.demo.service.exception;

public class InvalidFieldException extends RuntimeException {
	private static final long serialVersionUID = 6589756203991261047L;

	public InvalidFieldException(Object id) {
		super("Invalid field. id " + id);
	}

}
