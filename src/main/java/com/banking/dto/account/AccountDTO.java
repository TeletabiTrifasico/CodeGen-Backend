package com.banking.dto.account;

import com.banking.entity.Account;
import com.banking.enums.AccountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountDTO {
    private Long id;
    private String iban;
    private AccountType accountType;
    private BigDecimal balance;
    private BigDecimal absoluteLimit;
    private BigDecimal dayLimit;
    private BigDecimal transactionLimit;
    private boolean active;
    private LocalDateTime createdAt;
    private String ownerUsername;
    private String ownerFullName;

    public static AccountDTO from(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setIban(account.getIban());
        dto.setAccountType(account.getAccountType());
        dto.setBalance(account.getBalance());
        dto.setAbsoluteLimit(account.getAbsoluteLimit());
        dto.setDayLimit(account.getDayLimit());
        dto.setTransactionLimit(account.getTransactionLimit());
        dto.setActive(account.isActive());
        dto.setCreatedAt(account.getCreatedAt());
        if (account.getUser() != null) {
            dto.setOwnerUsername(account.getUser().getUsername());
            dto.setOwnerFullName(account.getUser().getFirstName() + " " + account.getUser().getLastName());
        }
        return dto;
    }
}
