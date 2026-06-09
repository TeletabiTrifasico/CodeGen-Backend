package com.banking.repository;

import com.banking.entity.Transaction;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("""
            SELECT t FROM Transaction t
            LEFT JOIN t.sourceAccount sa
            LEFT JOIN t.destinationAccount da
            WHERE sa.iban = :iban OR da.iban = :iban
            ORDER BY t.timestamp DESC
            """)
    Page<Transaction> findByAccountIban(@Param("iban") String iban, Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            LEFT JOIN t.sourceAccount sa
            LEFT JOIN sa.user sau
            LEFT JOIN t.destinationAccount da
            LEFT JOIN da.user dau
            WHERE sau.username = :username
               OR dau.username = :username
            ORDER BY t.timestamp DESC
            """)
    Page<Transaction> findByUsername(@Param("username") String username, Pageable pageable);

    Page<Transaction> findAllByOrderByTimestampDesc(Pageable pageable);

    // COALESCE(SUM(t.amount),0) => return 0 instead of null
    // if there are no transactions.
    @Query("""
                SELECT COALESCE(SUM(t.amount),0)
                FROM Transaction t
                WHERE t.sourceAccount.iban = :iban
                  AND t.timestamp >= :start
                  AND t.timestamp < :end
            """)
    BigDecimal sumOutgoingAmountByIbanAndDateRange(
            @Param("iban") String iban,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
