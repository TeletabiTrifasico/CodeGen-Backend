package com.banking.dto.account;

import com.banking.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountRequest {

    @NotNull
    private AccountType accountType;

    /** Employee can create account for a specific user; customers create for themselves */
    private Long userId;
}
