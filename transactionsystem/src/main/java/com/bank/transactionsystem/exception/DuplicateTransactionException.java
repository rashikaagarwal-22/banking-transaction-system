package com.bank.transactionsystem.exception;

public class DuplicateTransactionException extends RuntimeException {

	public DuplicateTransactionException(String message) {
		super(message);
	}

}
