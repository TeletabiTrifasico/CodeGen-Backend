package com.banking.entity;

import com.banking.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_account_iban", columnList = "iban"),
    @Index(name = "idx_account_user", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String iban;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Column(precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /** Minimum allowed balance (e.g. 0 = no overdraft, -1000 = €1000 overdraft allowed) */
    @Column(precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal absoluteLimit = BigDecimal.ZERO;

    /** Maximum total outgoing transfers per day */
    @Column(precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal dayLimit = new BigDecimal("1000.00");

    /** Maximum amount for a single transaction */
    @Column(precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal transactionLimit = new BigDecimal("500.00");

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;
}
