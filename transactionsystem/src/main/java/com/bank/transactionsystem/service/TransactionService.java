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

	@Transactional
	public TransactionRecord transfer(
			Long sourceAccountId,
			Long destinationAccountId,
			BigDecimal amount,
			String transactionId) {
		log.info("Starting transfer transactionId={} sourceAccountId={} destinationAccountId={} amount={}",
				transactionId, sourceAccountId, destinationAccountId, amount);

		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Transfer amount must be positive");
		}

		if (sourceAccountId == null || destinationAccountId == null || sourceAccountId.equals(destinationAccountId)) {
			throw new IllegalArgumentException("Source and destination accounts must be valid and distinct");
		}

		if (transactionRepository.existsByTransactionId(transactionId)) {
			throw new DuplicateTransactionException("Duplicate transactionId: " + transactionId);
		}

		Long firstId = sourceAccountId < destinationAccountId ? sourceAccountId : destinationAccountId;
		Long secondId = sourceAccountId < destinationAccountId ? destinationAccountId : sourceAccountId;

		Account firstAccount = lockAccountOrThrow(firstId);
		Account secondAccount = lockAccountOrThrow(secondId);

		Account sourceAccount = sourceAccountId.equals(firstId) ? firstAccount : secondAccount;
		Account destinationAccount = destinationAccountId.equals(firstId) ? firstAccount : secondAccount;

		requireActiveAccount(sourceAccount);
		requireActiveAccount(destinationAccount);

		if (sourceAccount.getBalance().compareTo(amount) < 0) {
			throw new InsufficientBalanceException("Insufficient balance for account: " + sourceAccountId);
		}

		sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
		destinationAccount.setBalance(destinationAccount.getBalance().add(amount));
		accountRepository.save(sourceAccount);
		accountRepository.save(destinationAccount);

		TransactionRecord savedRecord = saveTransactionRecord(
				transactionId, "TRANSFER", amount, sourceAccountId, destinationAccountId);
		log.info("Completed transfer transactionId={} sourceAccountId={} destinationAccountId={} sourceNewBalance={} destinationNewBalance={}",
				transactionId, sourceAccountId, destinationAccountId,
				sourceAccount.getBalance(), destinationAccount.getBalance());

		return savedRecord;
	}

	private Account lockAccountOrThrow(Long accountId) {
		return accountRepository.findByAccountIdForUpdate(accountId)
				.orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
	}

	private void requireActiveAccount(Account account) {
		if (!"ACTIVE".equals(account.getStatus())) {
			throw new IllegalArgumentException("Account is not ACTIVE: " + account.getAccountId());
		}
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
