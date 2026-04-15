package com.banking.service;

import com.banking.dto.transaction.TransactionDTO;
import com.banking.dto.transaction.TransferRequest;

import java.util.List;

public interface TransactionService {
    TransactionDTO transfer(TransferRequest request, String currentUsername);
    List<TransactionDTO> getTransactionsForCurrentUser(String username);
    List<TransactionDTO> getTransactionsByIban(String iban, String currentUsername);
    List<TransactionDTO> getAllTransactions();
}
