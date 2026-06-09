package com.banking.service.impl;

import com.banking.dto.transaction.TransactionDTO;
import com.banking.dto.transaction.TransferRequest;
import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.entity.User;
import com.banking.enums.TransactionType;
import com.banking.enums.UserRole;
import com.banking.exception.ResourceNotFoundException;
import com.banking.exception.UnauthorizedException;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import com.banking.service.TransactionService;
import com.banking.service.TransferPolicy.TransferPolicy;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransferPolicy transferPolicy;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Override
    public Page<TransactionDTO> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAllByOrderByTimestampDesc(pageable)
                .map(TransactionDTO::from);
    }

    @Override
    public Page<TransactionDTO> getTransactionsByUser(String username, Pageable pageable) {
        return transactionRepository.findByUsername(username, pageable)
                .map(TransactionDTO::from);
    }

    @Override
    public Page<TransactionDTO> getTransactionsByIban(
            String iban,
            String currentUsername,
            Pageable pageable) {

        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + iban));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser.getRole() != UserRole.EMPLOYEE &&
                !account.getUser().getUsername().equals(currentUsername)) {
            throw new UnauthorizedException("You do not have access to this account's transactions");
        }

        return transactionRepository.findByAccountIban(iban, pageable)
                .map(TransactionDTO::from);
    }

    @Override
    @Transactional
    public TransactionDTO transfer(TransferRequest request, String username) {

        User user = getUser(username);
        Account from = getAccount(request.getFromIban());
        Account to = getAccount(request.getToIban());
        BigDecimal amount = request.getAmount();

        BigDecimal todayOutgoing = calculateTodayOutgoing(from.getIban());

        transferPolicy.validate(user, from, to, amount, todayOutgoing);

        executeTransfer(from, to, amount);

        Transaction transaction = createTransaction(user, from, to, amount);

        accountRepository.save(from);
        accountRepository.save(to);

        return TransactionDTO.from(transactionRepository.save(transaction));
    }

    // Helper methods
    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Account getAccount(String iban) {
        return accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + iban));
    }

    private BigDecimal calculateTodayOutgoing(String iban) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        BigDecimal result = transactionRepository
                .sumOutgoingAmountByIbanAndDateRange(iban, start, end);

        return result != null ? result : BigDecimal.ZERO;
    }

    private void executeTransfer(Account from, Account to, BigDecimal amount) {
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
    }

    private Transaction createTransaction(User user, Account from, Account to, BigDecimal amount) {
        return Transaction.builder()
                .reference(generateReference())
                .sourceAccount(from)
                .destinationAccount(to)
                .amount(amount)
                .description("Transfer")
                .type(TransactionType.TRANSFER)
                .initiatedBy(user)
                .build();
    }

    private String generateReference() {
        return "TRX-" + UUID.randomUUID().toString()
                .substring(0, 8)
                .toUpperCase();
    }
}
