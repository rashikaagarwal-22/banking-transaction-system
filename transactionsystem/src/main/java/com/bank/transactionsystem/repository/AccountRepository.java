package com.bank.transactionsystem.repository;

import com.bank.transactionsystem.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
