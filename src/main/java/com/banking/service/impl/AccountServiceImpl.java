package com.banking.service.impl;

import com.banking.dto.account.*;
import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.enums.AccountType;
import com.banking.enums.UserRole;
import com.banking.exception.BadRequestException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.exception.UnauthorizedException;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import com.banking.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Override
    public List<AccountDTO> getAccountsOfCurrentUser(String username) {
        return accountRepository.findByUserUsername(username).stream()
                .map(AccountDTO::from)
                .toList();
    }

    @Override
    public List<AccountDTO> getAccountsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return accountRepository.findByUser(user).stream()
                .map(AccountDTO::from)
                .toList();
    }

    @Override
    public AccountDTO getAccountByIban(String iban) {
        return accountRepository.findByIban(iban)
                .map(AccountDTO::from)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + iban));
    }

    @Override
    @Transactional
    public AccountDTO createAccount(CreateAccountRequest request, String currentUsername) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User targetUser;
        if (currentUser.getRole() == UserRole.EMPLOYEE && request.getUserId() != null) {
            targetUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));
        } else {
            targetUser = currentUser;
        }

        if (targetUser.getRole() == UserRole.CUSTOMER && !targetUser.isApproved()) {
            throw new UnauthorizedException("Account cannot be created for an unapproved customer");
        }

        BigDecimal dayLimit = request.getAccountType() == AccountType.SAVINGS
                ? new BigDecimal("500.00") : new BigDecimal("1000.00");
        BigDecimal txLimit = request.getAccountType() == AccountType.SAVINGS
                ? new BigDecimal("250.00") : new BigDecimal("500.00");

        Account account = Account.builder()
                .iban(generateIban())
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO)
                .absoluteLimit(BigDecimal.ZERO)
                .dayLimit(dayLimit)
                .transactionLimit(txLimit)
                .active(true)
                .user(targetUser)
                .build();

        return AccountDTO.from(accountRepository.save(account));
    }

    @Override
    public List<AccountDTO> getAllCustomerAccounts() {
        return accountRepository.findByUserRole(UserRole.CUSTOMER).stream()
                .map(AccountDTO::from)
                .toList();
    }

    @Override
    @Transactional
    public AccountDTO updateAccount(String iban, UpdateAccountRequest request) {
        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + iban));
        if (request.getAbsoluteLimit() != null) {
            if (request.getAbsoluteLimit().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Absolute limit must be zero or positive");
            }
            account.setAbsoluteLimit(request.getAbsoluteLimit());
        }
        if (request.getDayLimit() != null) {
            if (request.getDayLimit().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Daily limit must be zero or positive");
            }
            account.setDayLimit(request.getDayLimit());
        }
        if (request.getTransactionLimit() != null) {
            if (request.getTransactionLimit().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Transaction limit must be zero or positive");
            }
            account.setTransactionLimit(request.getTransactionLimit());
        }
        account.setActive(request.isActive());
        return AccountDTO.from(accountRepository.save(account));
    }

    private String generateIban() {
        String iban;
        do {
            long num = (long) (Math.random() * 9_000_000_000L) + 1_000_000_000L;
            iban = "NL02BANK" + num;
        } while (accountRepository.existsByIban(iban));
        return iban;
    }
}
