package com.bank.transactionsystem.service;

import com.bank.transactionsystem.entity.Account;
import com.bank.transactionsystem.entity.TransactionRecord;
import com.bank.transactionsystem.exception.AccountNotFoundException;
import com.bank.transactionsystem.exception.DuplicateTransactionException;
import com.bank.transactionsystem.exception.InsufficientBalanceException;
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

		Account account = validateAndLockAccount(accountId, amount, transactionId);

		account.setBalance(account.getBalance().add(amount));
		accountRepository.save(account);

		TransactionRecord savedRecord = saveTransactionRecord(
				transactionId, "DEPOSIT", amount, null, accountId);
		log.info("Completed deposit transactionId={} accountId={} newBalance={}",
				transactionId, accountId, account.getBalance());

		return savedRecord;
	}

	@Transactional
	public TransactionRecord withdraw(Long accountId, BigDecimal amount, String transactionId) {
		log.info("Starting withdrawal transactionId={} accountId={} amount={}", transactionId, accountId, amount);

		Account account = validateAndLockAccount(accountId, amount, transactionId);

		if (account.getBalance().compareTo(amount) < 0) {
			throw new InsufficientBalanceException("Insufficient balance for account: " + accountId);
		}

		account.setBalance(account.getBalance().subtract(amount));
		accountRepository.save(account);

		TransactionRecord savedRecord = saveTransactionRecord(
				transactionId, "WITHDRAWAL", amount, accountId, null);
		log.info("Completed withdrawal transactionId={} accountId={} newBalance={}",
				transactionId, accountId, account.getBalance());

		return savedRecord;
	}

	private Account validateAndLockAccount(Long accountId, BigDecimal amount, String transactionId) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Transaction amount must be positive");
		}

		if (transactionRepository.existsByTransactionId(transactionId)) {
			throw new DuplicateTransactionException("Duplicate transactionId: " + transactionId);
		}

		Account account = accountRepository.findByAccountIdForUpdate(accountId)
				.orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

		if (!"ACTIVE".equals(account.getStatus())) {
			throw new IllegalArgumentException("Account is not ACTIVE: " + accountId);
		}

		return account;
	}

	private TransactionRecord saveTransactionRecord(
			String transactionId,
			String type,
			BigDecimal amount,
			Long sourceAccountId,
			Long destinationAccountId) {
		TransactionRecord record = new TransactionRecord();
		record.setTransactionId(transactionId);
		record.setType(type);
		record.setAmount(amount);
		record.setStatus("SUCCESS");
		record.setSourceAccountId(sourceAccountId);
		record.setDestinationAccountId(destinationAccountId);
		record.setCreatedAt(LocalDateTime.now());
		return transactionRepository.save(record);
	}

}
