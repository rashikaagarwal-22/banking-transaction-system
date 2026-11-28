package com.bank.transactionsystem.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AccountNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleAccountNotFound(AccountNotFoundException ex) {
		return buildErrorResponse(HttpStatus.NOT_FOUND, ex);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
		return buildErrorResponse(HttpStatus.BAD_REQUEST, ex);
	}

	private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, Exception ex) {
		String message = ex.getMessage() != null ? ex.getMessage() : status.getReasonPhrase();
		return ResponseEntity.status(status).body(Map.of("message", message));
	}

}
