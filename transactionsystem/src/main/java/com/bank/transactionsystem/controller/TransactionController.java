package com.bank.transactionsystem.controller;

import com.bank.transactionsystem.dto.DepositRequest;
import com.bank.transactionsystem.entity.TransactionRecord;
import com.bank.transactionsystem.service.TransactionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

	private final TransactionService transactionService;

	public TransactionController(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@PostMapping("/deposit")
	public TransactionRecord deposit(@RequestBody DepositRequest request) {
		return transactionService.deposit(
				request.getAccountId(),
				request.getAmount(),
				request.getTransactionId());
	}

	@PostMapping("/withdraw")
	public TransactionRecord withdraw(@RequestBody DepositRequest request) {
		return transactionService.withdraw(
				request.getAccountId(),
				request.getAmount(),
				request.getTransactionId());
	}

}
