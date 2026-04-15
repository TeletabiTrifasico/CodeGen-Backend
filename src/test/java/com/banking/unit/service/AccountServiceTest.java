package com.banking.unit.service;

import com.banking.dto.account.AccountDTO;
import com.banking.dto.account.CreateAccountRequest;
import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.enums.AccountType;
import com.banking.enums.UserRole;
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
import static org.mockito.Mockito.*;

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
            .role(UserRole.CUSTOMER)
            .approved(true)
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        pendingCustomer = User.builder()
            .id(2L)
            .username("pending")
            .role(UserRole.CUSTOMER)
            .approved(false)
            .dateOfBirth(LocalDate.of(1995, 1, 1))
            .build();
    }

    @Test
    void getAccountsForCurrentUser_returnsAccounts() {
        Account a1 = Account.builder()
            .id(1L).iban("NL02BANK1000000001")
            .accountType(AccountType.CHECKING)
            .balance(BigDecimal.TEN)
            .absoluteLimit(BigDecimal.ZERO)
            .dayLimit(new BigDecimal("1000"))
            .transactionLimit(new BigDecimal("500"))
            .user(approvedCustomer)
            .build();

        when(accountRepository.findByUserUsername("johndoe")).thenReturn(List.of(a1));

        List<AccountDTO> result = accountService.getAccountsForCurrentUser("johndoe");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIban()).isEqualTo("NL02BANK1000000001");
    }

    @Test
    void createAccount_pendingCustomer_throws() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType(AccountType.SAVINGS);

        when(userRepository.findByUsername("pending")).thenReturn(Optional.of(pendingCustomer));

        assertThatThrownBy(() -> accountService.createAccount(request, "pending"))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("unapproved");
    }

    @Test
    void createAccount_approvedCustomer_success() {
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

        assertThat(result.getAccountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(result.getIban()).isNotBlank();
    }
}
