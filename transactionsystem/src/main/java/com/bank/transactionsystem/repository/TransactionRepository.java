package com.bank.transactionsystem.repository;

import com.bank.transactionsystem.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {

	boolean existsByTransactionId(String transactionId);

}
