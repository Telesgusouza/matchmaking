package com.example.demo.repository.exception;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.demo.service.exception.InvalidFieldException;
import com.example.demo.service.exception.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class ResourceExceptionHandler {

	@ExceptionHandler(InvalidFieldException.class)
	public ResponseEntity<StandardError> handleInvalidFieldException(InvalidFieldException e, HttpServletRequest request) {
		
		String error = "Invalid field";
		Integer status = 400;
		StandardError err = new StandardError(Instant.now(), status, error, e.getMessage(), request.getRequestURI());
		
		return ResponseEntity.status(status).body(err);
	}
	
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<StandardError> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
		
		String error = "Resource Not Found";
		Integer status = 400;
		StandardError err = new StandardError(Instant.now(), status, error, e.getMessage(), request.getRequestURI());
		
		return ResponseEntity.status(status).body(err);
	}
	
}
