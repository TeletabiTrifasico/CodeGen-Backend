package com.banking.service;

import com.banking.dto.transaction.TransactionDTO;
import com.banking.dto.transaction.TransferRequest;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

public interface TransactionService {
    TransactionDTO transfer(TransferRequest request, String currentUsername);
    Page<TransactionDTO> getTransactionsForCurrentUser(String username, Pageable pageable);
    Page<TransactionDTO> getTransactionsByIban(String iban, String currentUsername, Pageable pageable);
    Page<TransactionDTO> getAllTransactions(Pageable pageable);
}
