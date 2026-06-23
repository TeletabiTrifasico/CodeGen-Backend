package com.banking.unit.service;

import com.banking.dto.account.AccountDTO;
import com.banking.dto.account.CreateAccountRequest;
import com.banking.dto.account.UpdateAccountRequest;
import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.enums.AccountType;
import com.banking.enums.UserRole;
import com.banking.exception.BadRequestException;
import com.banking.exception.UnauthorizedException;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import com.banking.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AccountServiceImpl}.
 *
 * These focus on the logic the SERVICE actually owns -- the limits it assigns
 * per account type, the ownership checks, and the limit validation on update.
 * Filtering done by repository queries (e.g. "find only customer accounts") is
 * the database's job, so we don't pretend to test it here; for the read methods
 * we only check that entities are mapped into DTOs correctly.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User approvedCustomer;
    private User pendingCustomer;

    @BeforeEach
    void setUp() {
        approvedCustomer = User.builder()
                .id(1L)
                .username("johndoe")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.CUSTOMER)
                .approved(true)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        pendingCustomer = User.builder()
                .id(2L)
                .username("pending")
                .firstName("Penny")
                .lastName("Ding")
                .role(UserRole.CUSTOMER)
                .approved(false)
                .dateOfBirth(LocalDate.of(1995, 1, 1))
                .build();
    }

    // ---------- createAccount: the service decides the limits per account type ----------

    @Test
    void createAccount_savings_appliesSavingsLimits() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType(AccountType.SAVINGS);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(approvedCustomer));
        when(accountRepository.existsByIban(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setId(10L);
            return a;
        });

        AccountDTO result = accountService.createAccount(request, "johndoe");

        // Limits/defaults below are chosen BY THE SERVICE, not by the test input.
        assertThat(result.getAccountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(result.getDayLimit()).isEqualByComparingTo("500.00");
        assertThat(result.getTransactionLimit()).isEqualByComparingTo("250.00");
        assertThat(result.getBalance()).isEqualByComparingTo("0");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getIban()).isNotBlank();
    }

    @Test
    void createAccount_checking_appliesCheckingLimits() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType(AccountType.CHECKING);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(approvedCustomer));
        when(accountRepository.existsByIban(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountDTO result = accountService.createAccount(request, "johndoe");

        assertThat(result.getAccountType()).isEqualTo(AccountType.CHECKING);
        assertThat(result.getDayLimit()).isEqualByComparingTo("1000.00");
        assertThat(result.getTransactionLimit()).isEqualByComparingTo("500.00");
    }

    @Test
    void createAccount_forUnapprovedCustomer_throwsUnauthorized() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType(AccountType.SAVINGS);

        when(userRepository.findByUsername("pending")).thenReturn(Optional.of(pendingCustomer));

        assertThatThrownBy(() -> accountService.createAccount(request, "pending"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("unapproved");

        verify(accountRepository, never()).save(any());
    }

    // ---------- getAccountByIban: ownership / authorization logic ----------

    @Test
    void getAccountByIban_owner_returnsAccount() {
        Account account = buildAccount(approvedCustomer);
        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(account));
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(approvedCustomer));

        AccountDTO result = accountService.getAccountByIban("NL02BANK1000000001", "johndoe");

        assertThat(result.getIban()).isEqualTo("NL02BANK1000000001");
        assertThat(result.getOwnerUsername()).isEqualTo("johndoe");
    }

    @Test
    void getAccountByIban_otherCustomer_throwsUnauthorized() {
        Account account = buildAccount(approvedCustomer); // owned by johndoe
        User intruder = User.builder()
                .id(3L).username("intruder").role(UserRole.CUSTOMER)
                .approved(true).dateOfBirth(LocalDate.of(1991, 1, 1)).build();

        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(account));
        when(userRepository.findByUsername("intruder")).thenReturn(Optional.of(intruder));

        assertThatThrownBy(() -> accountService.getAccountByIban("NL02BANK1000000001", "intruder"))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ---------- updateAccount: limit validation ----------

    @Test
    void updateAccount_negativeAbsoluteLimit_throwsBadRequest() {
        Account account = buildAccount(approvedCustomer);
        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(account));

        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setAbsoluteLimit(new BigDecimal("-1.00"));

        assertThatThrownBy(() -> accountService.updateAccount("NL02BANK1000000001", request))
                .isInstanceOf(BadRequestException.class);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void updateAccount_validLimits_areApplied() {
        Account account = buildAccount(approvedCustomer);
        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setDayLimit(new BigDecimal("3000.00"));
        request.setTransactionLimit(new BigDecimal("800.00"));

        AccountDTO result = accountService.updateAccount("NL02BANK1000000001", request);

        assertThat(result.getDayLimit()).isEqualByComparingTo("3000.00");
        assertThat(result.getTransactionLimit()).isEqualByComparingTo("800.00");
    }

    // ---------- read method: entity -> DTO mapping ----------

    @Test
    void getAllCustomerAccounts_mapsEntitiesToDtos() {
        Account a1 = buildAccount(approvedCustomer);
        Account a2 = Account.builder()
                .id(2L).iban("NL02BANK1000000002")
                .accountType(AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(500))
                .absoluteLimit(BigDecimal.ZERO)
                .dayLimit(new BigDecimal("500"))
                .transactionLimit(new BigDecimal("250"))
                .user(approvedCustomer)
                .build();

        when(accountRepository.findByUserRole(UserRole.CUSTOMER)).thenReturn(List.of(a1, a2));

        List<AccountDTO> result = accountService.getAllCustomerAccounts();

        assertThat(result).hasSize(2);
        // ownerFullName is built by AccountDTO.from ("first last") -- that's real mapping logic.
        assertThat(result.get(0).getOwnerFullName()).isEqualTo("John Doe");
        assertThat(result.get(0).getIban()).isEqualTo("NL02BANK1000000001");
        verify(accountRepository).findByUserRole(UserRole.CUSTOMER);
    }

    private Account buildAccount(User owner) {
        return Account.builder()
                .id(1L)
                .iban("NL02BANK1000000001")
                .accountType(AccountType.CHECKING)
                .balance(BigDecimal.TEN)
                .absoluteLimit(BigDecimal.ZERO)
                .dayLimit(new BigDecimal("1000"))
                .transactionLimit(new BigDecimal("500"))
                .active(true)
                .user(owner)
                .build();
    }
}
