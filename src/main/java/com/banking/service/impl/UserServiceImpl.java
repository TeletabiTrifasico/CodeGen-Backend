package com.banking.service.impl;

import com.banking.dto.user.UserDTO;
import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.enums.AccountType;
import com.banking.enums.UserRole;
import com.banking.exception.BadRequestException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import com.banking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
            .map(UserDTO::from)
            .toList();
    }

    @Override
    public List<UserDTO> getPendingCustomers() {
        return userRepository.findByApprovedAndRole(false, UserRole.CUSTOMER).stream()
            .map(UserDTO::from)
            .toList();
    }

    @Override
    public UserDTO getUserById(Long id) {
        return userRepository.findById(id)
            .map(UserDTO::from)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Override
    public List<UserDTO> getCustomersWithoutAccounts() {
        return userRepository.findByRoleAndAccountsIsEmpty(UserRole.CUSTOMER).stream()
            .map(UserDTO::from)
            .toList();
    }

    @Override
    @Transactional
    public UserDTO approveUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (user.getRole() != UserRole.CUSTOMER) {
            throw new BadRequestException("Only customers can be approved");
        }
        if (user.isApproved()) {
            throw new BadRequestException("User is already approved");
        }

        user.setApproved(true);
        userRepository.save(user);

        // Auto-create a CHECKING account for the newly approved customer
        Account checking = Account.builder()
            .iban(generateIban())
            .accountType(AccountType.CHECKING)
            .balance(BigDecimal.ZERO)
            .absoluteLimit(BigDecimal.ZERO)
            .dayLimit(new BigDecimal("1000.00"))
            .transactionLimit(new BigDecimal("500.00"))
            .active(true)
            .user(user)
            .build();
        accountRepository.save(checking);

        return UserDTO.from(user);
    }

    private String generateIban() {
        String iban;
        do {
            long accountNumber = (long) (Math.random() * 9_000_000_000L) + 1_000_000_000L;
            iban = "NL02BANK" + accountNumber;
        } while (accountRepository.existsByIban(iban));
        return iban;
    }
}
