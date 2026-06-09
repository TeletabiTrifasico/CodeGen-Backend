package com.banking.dto.account;

import com.banking.entity.Account;
import com.banking.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IbanSearchResultDTO {
    private String iban;
    private String ownerFullName;
    private AccountType accountType;

    public static IbanSearchResultDTO from(Account account) {
        String fullName = account.getUser() != null
            ? account.getUser().getFirstName() + " " + account.getUser().getLastName()
            : "";
        return new IbanSearchResultDTO(account.getIban(), fullName, account.getAccountType());
    }
}
