package com.banking.repository;

import com.banking.entity.Account;
import com.banking.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySourceAccountOrDestinationAccountOrderByTimestampDesc(
        Account sourceAccount, Account destinationAccount
    );

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.sourceAccount.iban = :iban OR t.destinationAccount.iban = :iban
        ORDER BY t.timestamp DESC
        """)
    List<Transaction> findByAccountIban(@Param("iban") String iban);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.sourceAccount.user.username = :username
           OR t.destinationAccount.user.username = :username
        ORDER BY t.timestamp DESC
        """)
    List<Transaction> findByUsername(@Param("username") String username);

    List<Transaction> findAllByOrderByTimestampDesc();
}
