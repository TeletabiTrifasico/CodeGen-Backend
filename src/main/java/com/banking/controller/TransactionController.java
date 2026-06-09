package com.banking.controller;

import com.banking.dto.transaction.TransactionDTO;
import com.banking.dto.transaction.TransferRequest;
import com.banking.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Transaction operations")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "Get all transactions involving the authenticated user's accounts")
    public ResponseEntity<List<TransactionDTO>> getMyTransactions(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(transactionService.getTransactionsForCurrentUser(userDetails.getUsername()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get all transactions in the system - Employee only")
    public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/account/{iban}")
    @Operation(summary = "Get transactions of a specific account")
    public ResponseEntity<List<TransactionDTO>> getByAccount(@PathVariable String iban,
                                                              @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(transactionService.getTransactionsByIban(iban, userDetails.getUsername()));
    }

    @PostMapping("/transaction")
    @Operation(summary = "Transfer money between two accounts")
    public ResponseEntity<TransactionDTO> transfer(@Valid @RequestBody TransferRequest request,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(transactionService.transfer(request, userDetails.getUsername()));
    }
}
