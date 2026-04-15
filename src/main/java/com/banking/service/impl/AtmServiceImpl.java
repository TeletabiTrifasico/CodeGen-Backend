package com.banking.service.impl;

import com.banking.dto.atm.AtmRequest;
import com.banking.dto.atm.AtmResponse;
import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.entity.User;
import com.banking.enums.TransactionType;
import com.banking.enums.UserRole;
import com.banking.exception.BadRequestException;
import com.banking.exception.InsufficientFundsException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.exception.UnauthorizedException;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import com.banking.service.AtmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AtmServiceImpl implements AtmService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AtmResponse deposit(AtmRequest request, String currentUsername) {
        Account account = resolveAndAuthorize(request.getIban(), currentUsername);

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        User user = userRepository.findByUsername(currentUsername).orElseThrow();
        Transaction tx = Transaction.builder()
            .reference("ATM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .destinationAccount(account)
            .amount(request.getAmount())
            .description(request.getDescription() != null ? request.getDescription() : "ATM Deposit")
            .type(TransactionType.ATM_DEPOSIT)
            .initiatedBy(user)
            .build();
        transactionRepository.save(tx);

        return new AtmResponse(tx.getReference(), account.getBalance(), "Deposit successful");
    }

    @Override
    @Transactional
    public AtmResponse withdraw(AtmRequest request, String currentUsername) {
        Account account = resolveAndAuthorize(request.getIban(), currentUsername);

        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        if (newBalance.compareTo(account.getAbsoluteLimit()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }
        if (request.getAmount().compareTo(account.getTransactionLimit()) > 0) {
            throw new BadRequestException("Amount exceeds transaction limit of " + account.getTransactionLimit());
        }

        account.setBalance(newBalance);
        accountRepository.save(account);

        User user = userRepository.findByUsername(currentUsername).orElseThrow();
        Transaction tx = Transaction.builder()
            .reference("ATM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .sourceAccount(account)
            .amount(request.getAmount())
            .description(request.getDescription() != null ? request.getDescription() : "ATM Withdrawal")
            .type(TransactionType.ATM_WITHDRAWAL)
            .initiatedBy(user)
            .build();
        transactionRepository.save(tx);

        return new AtmResponse(tx.getReference(), account.getBalance(), "Withdrawal successful");
    }

    private Account resolveAndAuthorize(String iban, String currentUsername) {
        Account account = accountRepository.findByIban(iban)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + iban));

        User currentUser = userRepository.findByUsername(currentUsername)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser.getRole() != UserRole.EMPLOYEE &&
            !account.getUser().getUsername().equals(currentUsername)) {
            throw new UnauthorizedException("You do not own this account");
        }
        if (!account.isActive()) throw new BadRequestException("Account is not active");
        return account;
    }
}
