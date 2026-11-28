package com.bank.transactionsystem.repository;

import com.bank.transactionsystem.entity.Account;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where a.accountId = :accountId")
	Optional<Account> findByAccountIdForUpdate(@Param("accountId") Long accountId);

}
