package com.banking.service;

import com.banking.dto.account.AccountDTO;
import com.banking.dto.account.CreateAccountRequest;

import java.util.List;

public interface AccountService {
    List<AccountDTO> getAccountsForCurrentUser(String username);
    List<AccountDTO> getAccountsByUserId(Long userId);
    AccountDTO getAccountByIban(String iban);
    AccountDTO createAccount(CreateAccountRequest request, String currentUsername);
}
