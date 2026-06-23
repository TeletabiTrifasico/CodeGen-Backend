package com.banking.unit.service;

import com.banking.dto.user.UserDTO;
import com.banking.entity.User;
import com.banking.enums.UserRole;
import com.banking.exception.BadRequestException;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import com.banking.entity.Account;
import com.banking.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserServiceImpl}.
 *
 * Note: the previous version used Java's {@code assert} keyword, which is
 * disabled unless the JVM is started with {@code -ea} (it is OFF by default in
 * IntelliJ run configurations). Those checks could silently pass even when
 * wrong. Here we use AssertJ's {@code assertThat(...)}, which always runs and
 * gives a readable failure message.
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User pendingCustomer;

    @BeforeEach
    void setUp() {
        pendingCustomer = User.builder()
                .id(1L)
                .username("pendingUser")
                .firstName("Penny")
                .lastName("Ding")
                .role(UserRole.CUSTOMER)
                .approved(false)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Test
    void getCustomersWithoutAccounts_mapsEntitiesToDtos() {
        when(userRepository.findByRoleAndAccountsIsEmpty(UserRole.CUSTOMER))
                .thenReturn(List.of(pendingCustomer));

        List<UserDTO> result = userService.getCustomersWithoutAccounts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("pendingUser");
        assertThat(result.get(0).getRole()).isEqualTo(UserRole.CUSTOMER);
        verify(userRepository).findByRoleAndAccountsIsEmpty(UserRole.CUSTOMER);
    }

    // ---------- approveUser: the real logic ----------

    @Test
    void approveUser_marksApprovedAndOpensTwoAccounts() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(pendingCustomer));
        when(accountRepository.existsByIban(anyString())).thenReturn(false);

        UserDTO result = userService.approveUser(1L);

        // The customer is now approved...
        assertThat(pendingCustomer.isApproved()).isTrue();
        assertThat(result.isApproved()).isTrue();
        verify(userRepository).save(pendingCustomer);
        // ...and the service opens a checking + savings account for them (2 saves).
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void approveUser_nonCustomer_throwsBadRequest() {
        User employee = User.builder()
                .id(2L).username("teller").role(UserRole.EMPLOYEE)
                .approved(false).dateOfBirth(LocalDate.of(1985, 1, 1)).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> userService.approveUser(2L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only customers");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void approveUser_alreadyApproved_throwsBadRequest() {
        pendingCustomer.setApproved(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(pendingCustomer));

        assertThatThrownBy(() -> userService.approveUser(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already approved");

        verify(accountRepository, never()).save(any());
    }
}
