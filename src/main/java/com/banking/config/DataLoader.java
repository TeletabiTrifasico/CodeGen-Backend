package com.banking.config;

import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.entity.User;
import com.banking.enums.AccountType;
import com.banking.enums.TransactionType;
import com.banking.enums.UserRole;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        log.info("Loading seed data...");

        // --- Employee ---
        User employee = userRepository.save(User.builder()
            .username("employee")
            .password(passwordEncoder.encode("Employee1!"))
            .firstName("Alice")
            .lastName("Smith")
            .email("alice@bank.nl")
            .phoneNumber("+31612345678")
            .bsn("123456789")
            .dateOfBirth(LocalDate.of(1985, 3, 15))
            .role(UserRole.EMPLOYEE)
            .approved(true)
            .build());

        // --- Approved customers ---
        User john = userRepository.save(User.builder()
            .username("johndoe")
            .password(passwordEncoder.encode("Password1!"))
            .firstName("John")
            .lastName("Doe")
            .email("john@example.nl")
            .phoneNumber("+31698765432")
            .bsn("987654321")
            .dateOfBirth(LocalDate.of(1990, 6, 20))
            .role(UserRole.CUSTOMER)
            .approved(true)
            .build());

        User jane = userRepository.save(User.builder()
            .username("janedoe")
            .password(passwordEncoder.encode("Password1!"))
            .firstName("Jane")
            .lastName("Doe")
            .email("jane@example.nl")
            .phoneNumber("+31611223344")
            .bsn("112233445")
            .dateOfBirth(LocalDate.of(1993, 11, 5))
            .role(UserRole.CUSTOMER)
            .approved(true)
            .build());

        // --- Pending customer ---
        userRepository.save(User.builder()
            .username("pending")
            .password(passwordEncoder.encode("Password1!"))
            .firstName("Bob")
            .lastName("Builder")
            .email("bob@example.nl")
            .phoneNumber("+31644556677")
            .bsn("556677889")
            .dateOfBirth(LocalDate.of(1998, 2, 14))
            .role(UserRole.CUSTOMER)
            .approved(false)
            .build());

        // --- Accounts for John ---
        Account johnChecking = accountRepository.save(Account.builder()
            .iban("NL02BANK1000000001")
            .accountType(AccountType.CHECKING)
            .balance(new BigDecimal("2500.00"))
            .absoluteLimit(BigDecimal.ZERO)
            .dayLimit(new BigDecimal("1000.00"))
            .transactionLimit(new BigDecimal("500.00"))
            .user(john)
            .build());

        Account johnSavings = accountRepository.save(Account.builder()
            .iban("NL02BANK1000000002")
            .accountType(AccountType.SAVINGS)
            .balance(new BigDecimal("10000.00"))
            .absoluteLimit(BigDecimal.ZERO)
            .dayLimit(new BigDecimal("500.00"))
            .transactionLimit(new BigDecimal("250.00"))
            .user(john)
            .build());

        // --- Accounts for Jane ---
        Account janeChecking = accountRepository.save(Account.builder()
            .iban("NL02BANK2000000001")
            .accountType(AccountType.CHECKING)
            .balance(new BigDecimal("1200.00"))
            .absoluteLimit(BigDecimal.ZERO)
            .dayLimit(new BigDecimal("1000.00"))
            .transactionLimit(new BigDecimal("500.00"))
            .user(jane)
            .build());

        // --- Sample transactions ---
        transactionRepository.save(Transaction.builder()
            .reference("TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .sourceAccount(johnChecking)
            .destinationAccount(janeChecking)
            .amount(new BigDecimal("200.00"))
            .description("Rent payment")
            .type(TransactionType.TRANSFER)
            .initiatedBy(john)
            .build());

        transactionRepository.save(Transaction.builder()
            .reference("ATM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .destinationAccount(johnChecking)
            .amount(new BigDecimal("500.00"))
            .description("ATM Deposit")
            .type(TransactionType.ATM_DEPOSIT)
            .initiatedBy(john)
            .build());

        transactionRepository.save(Transaction.builder()
            .reference("ATM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .sourceAccount(janeChecking)
            .amount(new BigDecimal("100.00"))
            .description("ATM Withdrawal")
            .type(TransactionType.ATM_WITHDRAWAL)
            .initiatedBy(jane)
            .build());

        transactionRepository.save(Transaction.builder()
            .reference("TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .sourceAccount(johnChecking)
            .destinationAccount(johnSavings)
            .amount(new BigDecimal("300.00"))
            .description("Monthly savings")
            .type(TransactionType.TRANSFER)
            .initiatedBy(john)
            .build());

        log.info("Seed data loaded: 1 employee, 2 approved customers, 1 pending customer, 3 accounts, 4 transactions");
    }
}
