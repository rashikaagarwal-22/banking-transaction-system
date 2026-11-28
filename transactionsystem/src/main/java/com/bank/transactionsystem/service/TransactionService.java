package com.bank.transactionsystem.service;

import com.bank.transactionsystem.entity.Account;
import com.bank.transactionsystem.entity.TransactionRecord;
import com.bank.transactionsystem.exception.AccountNotFoundException;
import com.bank.transactionsystem.repository.AccountRepository;
import com.bank.transactionsystem.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

	private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;

	public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
	}

	@Transactional
	public TransactionRecord deposit(Long accountId, BigDecimal amount, String transactionId) {
		log.info("Starting deposit transactionId={} accountId={} amount={}", transactionId, accountId, amount);

		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Deposit amount must be positive");
		}

		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

		if (!"ACTIVE".equals(account.getStatus())) {
			throw new IllegalArgumentException("Account is not ACTIVE: " + accountId);
		}

		account.setBalance(account.getBalance().add(amount));
		accountRepository.save(account);

		TransactionRecord record = new TransactionRecord();
		record.setTransactionId(transactionId);
		record.setType("DEPOSIT");
		record.setAmount(amount);
		record.setStatus("SUCCESS");
		record.setDestinationAccountId(accountId);
		record.setCreatedAt(LocalDateTime.now());

		TransactionRecord savedRecord = transactionRepository.save(record);
		log.info("Completed deposit transactionId={} accountId={} newBalance={}",
				transactionId, accountId, account.getBalance());

		return savedRecord;
	}

}
