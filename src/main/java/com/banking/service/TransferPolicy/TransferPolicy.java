package com.banking.service.TransferPolicy;

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
public class TransferPolicy {

    public void validate(User user,
            Account from,
            Account to,
            BigDecimal amount,
            BigDecimal todayOutgoing) {

        validateSameAccount(from, to);
        validateOwnership(user, from);
        validateAccountStatus(from, to);
        validateBusinessRules(from, to);
        validateTransactionLimit(from, amount);
        validateDailyLimit(from, amount, todayOutgoing);
        validateAbsoluteLimit(from, amount);
    }

    private void validateSameAccount(@NonNull Account from, @NonNull Account to) {
        if (from.getIban().equals(to.getIban())) {
            throw new BadRequestException("Cannot transfer to the same account");
        }
    }

    private void validateOwnership(@NonNull User user, @NonNull Account from) {
        if (user.getRole() != UserRole.EMPLOYEE &&
                !from.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own the source account");
        }
    }

    private void validateAccountStatus(@NonNull Account from, @NonNull Account to) {
        if (!from.isActive()) {
            throw new BadRequestException("Source account is not active");
        }
        if (!to.isActive()) {
            throw new BadRequestException("Destination account is not active");
        }
    }

    private void validateBusinessRules(@NonNull Account from, @NonNull Account to) {
        if (isSameUser(from, to))
            return;

        if (from.getAccountType() != AccountType.CHECKING ||
                to.getAccountType() != AccountType.CHECKING) {
            throw new BadRequestException(
                    "External transfers are only allowed between checking accounts");
        }
    }

    private boolean isSameUser(@NonNull Account from, @NonNull Account to) {
        return from.getUser().getId().equals(to.getUser().getId());
    }

    private void validateTransactionLimit(@NonNull Account from, @NonNull BigDecimal amount) {
        if (amount.compareTo(from.getTransactionLimit()) > 0) {
            throw new BadRequestException(
                    "Amount exceeds per-transaction limit: " + from.getTransactionLimit());
        }
    }

    private void validateDailyLimit(@NonNull Account from, @NonNull BigDecimal amount, @NonNull BigDecimal todayOutgoing) {
        BigDecimal total = todayOutgoing.add(amount);

        if (total.compareTo(from.getDayLimit()) > 0) {
            throw new BadRequestException(
                    "Daily limit exceeded. Limit: " + from.getDayLimit() +
                            ", already used: " + todayOutgoing);
        }
    }

    private void validateAbsoluteLimit(@NonNull Account from, @NonNull BigDecimal amount) {
        BigDecimal newBalance = from.getBalance().subtract(amount);

        if (newBalance.compareTo(from.getAbsoluteLimit()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds (absolute limit reached)");
        }
    }
}