package com.banking.service;

import com.banking.dto.account.*;

import java.util.List;

public interface AccountService {
    List<AccountDTO> getAccountsOfCurrentUser(String username);
    List<AccountDTO> getAccountsByUserId(Long userId);
    AccountDTO getAccountByIban(String iban);
    AccountDTO createAccount(CreateAccountRequest request, String currentUsername);
    List<AccountDTO> getAllCustomerAccounts();
    AccountDTO updateAccount(String iban, UpdateAccountRequest request);
}
