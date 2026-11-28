package com.bank.transactionsystem;

import com.bank.transactionsystem.entity.Account;
import com.bank.transactionsystem.exception.AccountNotFoundException;
import com.bank.transactionsystem.exception.DuplicateTransactionException;
import com.bank.transactionsystem.exception.InsufficientBalanceException;
import com.bank.transactionsystem.repository.AccountRepository;
import com.bank.transactionsystem.repository.TransactionRepository;
import com.bank.transactionsystem.service.TransactionService;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ConcurrencyTest {

	@Autowired
	private TransactionService transactionService;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Account createAccountWithBalance(BigDecimal balance) {
		Long customerId = jdbcTemplate.queryForObject(
				"SELECT customer_id FROM customer ORDER BY customer_id LIMIT 1",
				Long.class);

		Account account = new Account();
		account.setCustomerId(customerId);
		account.setBalance(balance);
		account.setStatus("ACTIVE");
		return accountRepository.save(account);
	}

	@RepeatedTest(20)
	void onlyOneWithdrawalSucceeds() throws InterruptedException, ExecutionException {
		Account account = createAccountWithBalance(new BigDecimal("10000"));
		Long accountId = account.getAccountId();

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch readyLatch = new CountDownLatch(2);
		CountDownLatch startLatch = new CountDownLatch(1);
		AtomicInteger successes = new AtomicInteger(0);
		AtomicInteger failures = new AtomicInteger(0);

		try {
			Future<?> futureA = submitWithdrawal(executor, readyLatch, startLatch,
					accountId, new BigDecimal("7000"), successes, failures);
			Future<?> futureB = submitWithdrawal(executor, readyLatch, startLatch,
					accountId, new BigDecimal("5000"), successes, failures);

			readyLatch.await();
			startLatch.countDown();

			futureA.get();
			futureB.get();
		} finally {
			executor.shutdownNow();
		}

		assertEquals(1, successes.get());
		assertEquals(1, failures.get());

		BigDecimal finalBalance = accountRepository.findById(accountId)
				.orElseThrow()
				.getBalance();
		boolean expectedBalance = finalBalance.compareTo(new BigDecimal("3000")) == 0
				|| finalBalance.compareTo(new BigDecimal("5000")) == 0;
		assertTrue(expectedBalance, "Expected final balance 3000 or 5000, but was " + finalBalance);
		assertTrue(finalBalance.compareTo(BigDecimal.ZERO) >= 0,
				"Balance must not be negative, but was " + finalBalance);
	}

	@Test
	void sameTransactionIdChangesMoneyOnce() {
		Account account = createAccountWithBalance(new BigDecimal("1000"));
		Long accountId = account.getAccountId();
		String transactionId = UUID.randomUUID().toString();
		BigDecimal depositAmount = new BigDecimal("100");

		transactionService.deposit(accountId, depositAmount, transactionId);

		assertThrows(DuplicateTransactionException.class,
				() -> transactionService.deposit(accountId, depositAmount, transactionId));

		BigDecimal finalBalance = accountRepository.findById(accountId)
				.orElseThrow()
				.getBalance();
		assertEquals(0, finalBalance.compareTo(new BigDecimal("1100")),
				"Expected balance 1100, not 1200, but was " + finalBalance);
	}

	@Test
	void failedTransferChangesNothing() {
		Account sourceAccount = createAccountWithBalance(new BigDecimal("1000"));
		Long sourceAccountId = sourceAccount.getAccountId();

		assertThrows(AccountNotFoundException.class,
				() -> transactionService.transfer(
						sourceAccountId,
						999999L,
						new BigDecimal("100"),
						UUID.randomUUID().toString()));

		BigDecimal finalBalance = accountRepository.findById(sourceAccountId)
				.orElseThrow()
				.getBalance();
		assertEquals(0, finalBalance.compareTo(new BigDecimal("1000")),
				"Expected balance to remain 1000, but was " + finalBalance);
	}

	private Future<?> submitWithdrawal(
			ExecutorService executor,
			CountDownLatch readyLatch,
			CountDownLatch startLatch,
			Long accountId,
			BigDecimal amount,
			AtomicInteger successes,
			AtomicInteger failures) {
		return executor.submit(() -> {
			readyLatch.countDown();
			try {
				startLatch.await();
				transactionService.withdraw(accountId, amount, UUID.randomUUID().toString());
				successes.incrementAndGet();
			} catch (InsufficientBalanceException e) {
				failures.incrementAndGet();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		});
	}

}
