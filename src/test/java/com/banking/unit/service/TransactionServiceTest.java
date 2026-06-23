package com.banking.unit.service;

import com.banking.dto.transaction.TransactionDTO;
import com.banking.dto.transaction.TransferRequest;
import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.entity.User;
import com.banking.enums.AccountType;
import com.banking.enums.TransactionType;
import com.banking.enums.UserRole;
import com.banking.exception.BadRequestException;
import com.banking.exception.InsufficientFundsException;
import com.banking.exception.UnauthorizedException;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import com.banking.service.TransferPolicy.TransferPolicy;
import com.banking.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TransactionServiceImpl}.
 *
 * Design note: we deliberately use the REAL {@link TransferPolicy} (the class
 * that holds the transfer rules) and only mock the repositories. That way the
 * tests exercise the actual validation + balance logic instead of a stand-in.
 *
 * The service is built by hand in {@link #setUp()} instead of using
 * {@code @InjectMocks}. {@code @InjectMocks} could not supply the real
 * TransferPolicy (it is not a mock), which left it null at runtime and made the
 * previous version of this test fail with a NullPointerException.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    private TransactionServiceImpl transactionService;

    private User customer;
    private Account fromAccount;
    private Account toAccount;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        TransferPolicy transferPolicy = new TransferPolicy(); // real rules, not a mock
        transactionService = new TransactionServiceImpl(
                transferPolicy,
                transactionRepository,
                accountRepository,
                userRepository);

        customer = User.builder()
                .id(1L)
                .username("johndoe")
                .role(UserRole.CUSTOMER)
                .approved(true)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        fromAccount = Account.builder()
                .id(1L)
                .iban("NL02BANK1000000001")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .dayLimit(new BigDecimal("2000.00"))
                .absoluteLimit(BigDecimal.ZERO)
                .transactionLimit(new BigDecimal("500.00"))
                .active(true)
                .user(customer)
                .build();

        User otherCustomer = User.builder()
                .id(2L)
                .username("janedoe")
                .role(UserRole.CUSTOMER)
                .approved(true)
                .dateOfBirth(LocalDate.of(1993, 1, 1))
                .build();

        toAccount = Account.builder()
                .id(2L)
                .iban("NL02BANK2000000001")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("500.00"))
                .absoluteLimit(BigDecimal.ZERO)
                .dayLimit(new BigDecimal("2000.00"))
                .transactionLimit(new BigDecimal("500.00"))
                .active(true)
                .user(otherCustomer)
                .build();

        transferRequest = new TransferRequest();
        transferRequest.setFromIban("NL02BANK1000000001");
        transferRequest.setToIban("NL02BANK2000000001");
        transferRequest.setAmount(new BigDecimal("200.00"));
        transferRequest.setDescription("Test transfer");
    }

    @Test
    void transfer_movesMoneyBetweenAccountsAndRecordsTransaction() {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(customer));
        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIban("NL02BANK2000000001")).thenReturn(Optional.of(toAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionDTO result = transactionService.transfer(transferRequest, "johndoe");

        // Balances are recomputed by the service: 1000 - 200 and 500 + 200.
        assertThat(fromAccount.getBalance()).isEqualByComparingTo("800.00");
        assertThat(toAccount.getBalance()).isEqualByComparingTo("700.00");

        assertThat(result.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(result.getReference()).isNotBlank();
    }

    @Test
    void transfer_insufficientFunds_throws() {
        fromAccount.setBalance(new BigDecimal("100.00")); // less than the 200 requested

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(customer));
        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIban("NL02BANK2000000001")).thenReturn(Optional.of(toAccount));

        assertThatThrownBy(() -> transactionService.transfer(transferRequest, "johndoe"))
                .isInstanceOf(InsufficientFundsException.class);

        // The money must not move when the transfer is rejected.
        assertThat(fromAccount.getBalance()).isEqualByComparingTo("100.00");
        assertThat(toAccount.getBalance()).isEqualByComparingTo("500.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_exceedsTransactionLimit_throws() {
        fromAccount.setBalance(new BigDecimal("10000.00"));
        transferRequest.setAmount(new BigDecimal("600.00")); // over the 500 per-transaction limit

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(customer));
        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIban("NL02BANK2000000001")).thenReturn(Optional.of(toAccount));

        assertThatThrownBy(() -> transactionService.transfer(transferRequest, "johndoe"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("transaction limit");
    }

    @Test
    void transfer_exceedsDailyLimit_throws() {
        // 1900 already sent today + 200 now = 2100, over the 2000 daily limit.
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(customer));
        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIban("NL02BANK2000000001")).thenReturn(Optional.of(toAccount));
        when(transactionRepository.sumOutgoingAmountByIbanAndDateRange(
                eq("NL02BANK1000000001"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("1900.00"));

        assertThatThrownBy(() -> transactionService.transfer(transferRequest, "johndoe"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Daily limit");
    }

    @Test
    void transfer_sameAccount_throws() {
        transferRequest.setToIban("NL02BANK1000000001"); // from == to

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(customer));
        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(fromAccount));

        assertThatThrownBy(() -> transactionService.transfer(transferRequest, "johndoe"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("same account");
    }

    @Test
    void transfer_notOwnerOfSourceAccount_throws() {
        // janedoe tries to transfer FROM johndoe's account.
        when(userRepository.findByUsername("janedoe")).thenReturn(Optional.of(toAccount.getUser()));
        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIban("NL02BANK2000000001")).thenReturn(Optional.of(toAccount));

        assertThatThrownBy(() -> transactionService.transfer(transferRequest, "janedoe"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
