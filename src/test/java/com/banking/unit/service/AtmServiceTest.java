package com.banking.unit.service;

import com.banking.dto.atm.AtmRequest;
import com.banking.dto.atm.AtmResponse;
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
import com.banking.service.impl.AtmServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtmServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AtmServiceImpl atmService;

    private User owner;
    private Account account;
    private AtmRequest request;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .username("johndoe")
                .role(UserRole.CUSTOMER)
                .approved(true)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        account = Account.builder()
                .id(1L)
                .iban("NL02BANK1000000001")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .absoluteLimit(BigDecimal.ZERO)
                .dayLimit(new BigDecimal("2000.00"))
                .transactionLimit(new BigDecimal("500.00"))
                .active(true)
                .user(owner)
                .build();

        request = new AtmRequest();
        request.setIban("NL02BANK1000000001");
        request.setAmount(new BigDecimal("200.00"));
        request.setDescription("ATM test");
    }

    @Test
    void deposit_addsAmountToBalanceAndRecordsTransaction() {
        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(account));
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        AtmResponse response = atmService.deposit(request, "johndoe");

        assertThat(account.getBalance()).isEqualByComparingTo("1200.00");
        assertThat(response.getNewBalance()).isEqualByComparingTo("1200.00");
        assertThat(response.getReference()).isNotBlank();

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction saved = txCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(TransactionType.ATM_DEPOSIT);
        assertThat(saved.getAmount()).isEqualByComparingTo("200.00");
        assertThat(saved.getDestinationAccount()).isEqualTo(account);
    }

    @Test
    void withdraw_subtractsAmountFromBalanceAndRecordsTransaction() {
        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(account));
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        AtmResponse response = atmService.withdraw(request, "johndoe");

        assertThat(account.getBalance()).isEqualByComparingTo("800.00");
        assertThat(response.getNewBalance()).isEqualByComparingTo("800.00");

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction saved = txCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(TransactionType.ATM_WITHDRAWAL);
        assertThat(saved.getSourceAccount()).isEqualTo(account);
    }

    @Test
    void withdraw_belowAbsoluteLimit_throwsInsufficientFunds() {
        account.setBalance(new BigDecimal("100.00"));
        request.setAmount(new BigDecimal("200.00"));

        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(account));
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> atmService.withdraw(request, "johndoe"))
                .isInstanceOf(InsufficientFundsException.class);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_aboveTransactionLimit_throwsBadRequest() {
        account.setBalance(new BigDecimal("10000.00"));
        request.setAmount(new BigDecimal("600.00"));

        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(account));
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> atmService.withdraw(request, "johndoe"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("transaction limit");
    }

    @Test
    void deposit_intoSomeoneElsesAccountAsCustomer_throwsUnauthorized() {
        User attacker = User.builder()
                .id(2L)
                .username("intruder")
                .role(UserRole.CUSTOMER)
                .approved(true)
                .dateOfBirth(LocalDate.of(1992, 1, 1))
                .build();

        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(account));
        when(userRepository.findByUsername("intruder")).thenReturn(Optional.of(attacker));

        assertThatThrownBy(() -> atmService.deposit(request, "intruder"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void deposit_intoInactiveAccount_throwsBadRequest() {
        account.setActive(false);

        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(account));
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> atmService.deposit(request, "johndoe"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void deposit_byEmployeeIntoCustomerAccount_isAllowed() {
        User employee = User.builder()
                .id(9L)
                .username("teller")
                .role(UserRole.EMPLOYEE)
                .approved(true)
                .dateOfBirth(LocalDate.of(1985, 1, 1))
                .build();

        when(accountRepository.findByIban("NL02BANK1000000001")).thenReturn(Optional.of(account));
        when(userRepository.findByUsername("teller")).thenReturn(Optional.of(employee));

        AtmResponse response = atmService.deposit(request, "teller");

        assertThat(account.getBalance()).isEqualByComparingTo("1200.00");
        assertThat(response.getNewBalance()).isEqualByComparingTo("1200.00");
    }
}
