package com.banking.service.impl;

import com.banking.dto.account.AccountDTO;
import com.banking.dto.account.CreateAccountRequest;
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
    public List<AccountDTO> getAccountsForCurrentUser(String username) {
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

    private String generateIban() {
        String iban;
        do {
            long num = (long) (Math.random() * 9_000_000_000L) + 1_000_000_000L;
            iban = "NL02BANK" + num;
        } while (accountRepository.existsByIban(iban));
        return iban;
    }
}
