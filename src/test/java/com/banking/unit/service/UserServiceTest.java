package com.banking.unit.service;

import com.banking.dto.user.UserDTO;
import com.banking.entity.User;
import com.banking.enums.UserRole;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import com.banking.service.UserService;
import com.banking.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountRepository accountRepository;
    @InjectMocks
    private UserServiceImpl userService;

    private User pending;

    @Test
    void getCustomersWithoutAccounts_returnsOnlyCustomersWithoutAccounts() {
        pending = User.builder()
            .id(1L)
            .username("pendingUser")
                .role(UserRole.CUSTOMER)
                .approved(false)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        when(userRepository.findByRoleAndAccountsIsEmpty(UserRole.CUSTOMER)).thenReturn(List.of(pending));

        List<UserDTO> result = userService.getCustomersWithoutAccounts();
        assert(result.size() == 1);
        assert(result.get(0).getUsername().equals("pendingUser"));
        verify(userRepository).findByRoleAndAccountsIsEmpty(UserRole.CUSTOMER);
    }
}
