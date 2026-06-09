package com.banking.service.TransferValidator;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.enums.AccountType;
import com.banking.enums.UserRole;
import com.banking.exception.BadRequestException;
import com.banking.exception.InsufficientFundsException;
import com.banking.exception.UnauthorizedException;


@Component
@RequiredArgsConstructor
public class TransferValidator {

    public void validateTransfer(User currentUser, Account from, Account to, BigDecimal amount,BigDecimal todayOutGoing, String currentUsername) {
        validateSameAccount(from, to);
        validateOwnership(currentUser, from, currentUsername);
        validateActiveAccounts(from, to);
        validateCheckingAccounts(from, to);
        validateTransactionLimit(from, amount);
        validateDailyLimit(from, amount,todayOutGoing);
        validateAbsoluteLimit(from, amount);
    }

    public boolean isSameUserTransfer(@NonNull Account from, @NonNull Account to) {
        return from.getUser().getId().equals(to.getUser().getId());
    }

    private void validateSameAccount(@NonNull Account from, @NonNull Account to) {
        if (from.getIban().equals(to.getIban())) {
            throw new BadRequestException("Cannot transfer to the same account");
        }
    }

    private void validateOwnership(@NonNull User currentUser, @NonNull Account from, String currentUsername) {
        if (currentUser.getRole() != UserRole.EMPLOYEE &&
                !from.getUser().getUsername().equals(currentUsername)) {
            throw new UnauthorizedException("You do not own the source account");
        }
    }

    private void validateActiveAccounts(@NonNull Account from, @NonNull Account to) {
        if (!from.isActive()) {
            throw new BadRequestException("Source account is not active");
        }
        if (!to.isActive()) {
            throw new BadRequestException("Destination account is not active");
        }
    }

    private void validateCheckingAccounts(@NonNull Account from, @NonNull Account to) {
        // Own-account transfers allow any combination of CHECKING/SAVINGS
        if (isSameUserTransfer(from, to)) return;
        if (from.getAccountType() != AccountType.CHECKING || to.getAccountType() != AccountType.CHECKING) {
            throw new BadRequestException("External transfers are only allowed between checking accounts");
        }
    }

    private void validateTransactionLimit(@NonNull Account from, @NonNull BigDecimal amount) {
        if (amount.compareTo(from.getTransactionLimit()) > 0) {
            throw new BadRequestException("Amount exceeds per-transaction limit of " + from.getTransactionLimit());
        }
    }

    private void validateDailyLimit(@NonNull Account from, BigDecimal amount,BigDecimal todayOutgoing) {
        if (todayOutgoing.add(amount).compareTo(from.getDayLimit()) > 0) {
            throw new BadRequestException(
                    "Daily transfer limit exceeded (limit: " + from.getDayLimit() +
                            ", already transferred today: " + todayOutgoing + ")"
            );
        }
    }

    private void validateAbsoluteLimit(@NonNull Account from, @NonNull BigDecimal amount) {
        BigDecimal newBalance = from.getBalance().subtract(amount);
        if (newBalance.compareTo(from.getAbsoluteLimit()) < 0) {
            throw new InsufficientFundsException("Insufficient funds (absolute limit: " + from.getAbsoluteLimit() + ")");
        }
    }
}