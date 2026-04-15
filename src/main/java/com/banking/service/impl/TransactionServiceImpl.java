package com.banking.service.impl;

import com.banking.dto.transaction.TransactionDTO;
import com.banking.dto.transaction.TransferRequest;
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
import com.banking.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TransactionDTO transfer(TransferRequest request, String currentUsername) {
        User currentUser = userRepository.findByUsername(currentUsername)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account from = accountRepository.findByIban(request.getFromIban())
            .orElseThrow(() -> new ResourceNotFoundException("Source account not found: " + request.getFromIban()));
        Account to = accountRepository.findByIban(request.getToIban())
            .orElseThrow(() -> new ResourceNotFoundException("Destination account not found: " + request.getToIban()));

        if (from.getIban().equals(to.getIban())) {
            throw new BadRequestException("Cannot transfer to the same account");
        }

        // Only the account owner (or employee) can initiate a transfer from an account
        if (currentUser.getRole() != UserRole.EMPLOYEE &&
            !from.getUser().getUsername().equals(currentUsername)) {
            throw new UnauthorizedException("You do not own the source account");
        }

        if (!from.isActive()) throw new BadRequestException("Source account is not active");
        if (!to.isActive()) throw new BadRequestException("Destination account is not active");

        BigDecimal amount = request.getAmount();

        if (amount.compareTo(from.getTransactionLimit()) > 0) {
            throw new BadRequestException("Amount exceeds per-transaction limit of " + from.getTransactionLimit());
        }

        BigDecimal newBalance = from.getBalance().subtract(amount);
        if (newBalance.compareTo(from.getAbsoluteLimit()) < 0) {
            throw new InsufficientFundsException("Insufficient funds (absolute limit: " + from.getAbsoluteLimit() + ")");
        }

        from.setBalance(newBalance);
        to.setBalance(to.getBalance().add(amount));
        accountRepository.save(from);
        accountRepository.save(to);

        Transaction transaction = Transaction.builder()
            .reference("TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .sourceAccount(from)
            .destinationAccount(to)
            .amount(amount)
            .description(request.getDescription())
            .type(TransactionType.TRANSFER)
            .initiatedBy(currentUser)
            .build();

        return TransactionDTO.from(transactionRepository.save(transaction));
    }

    @Override
    public List<TransactionDTO> getTransactionsForCurrentUser(String username) {
        return transactionRepository.findByUsername(username).stream()
            .map(TransactionDTO::from)
            .toList();
    }

    @Override
    public List<TransactionDTO> getTransactionsByIban(String iban, String currentUsername) {
        Account account = accountRepository.findByIban(iban)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + iban));

        User currentUser = userRepository.findByUsername(currentUsername)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only the account owner or an employee can view account transactions
        if (currentUser.getRole() != UserRole.EMPLOYEE &&
            !account.getUser().getUsername().equals(currentUsername)) {
            throw new UnauthorizedException("You do not have access to this account's transactions");
        }

        return transactionRepository.findByAccountIban(iban).stream()
            .map(TransactionDTO::from)
            .toList();
    }

    @Override
    public List<TransactionDTO> getAllTransactions() {
        return transactionRepository.findAllByOrderByTimestampDesc().stream()
            .map(TransactionDTO::from)
            .toList();
    }
}
