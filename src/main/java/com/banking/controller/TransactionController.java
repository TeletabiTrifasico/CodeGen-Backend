package com.banking.controller;

import com.banking.dto.transaction.TransactionDTO;
import com.banking.dto.transaction.TransferRequest;
import com.banking.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Transaction operations")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE')")
    @Operation(summary = "Get transactions. Employees see all, customers see their own.")
    public ResponseEntity<Page<TransactionDTO>> getTransactions(
            @RequestParam(required = false) String iban,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isEmployee = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

        if (iban != null) {
            return ResponseEntity
                    .ok(transactionService.getTransactionsByIban(iban, userDetails.getUsername(), pageable));
        }

        if (isEmployee) {
            return ResponseEntity.ok(transactionService.getAllTransactions(pageable));
        }

        return ResponseEntity.ok(transactionService.getTransactionsByUser(userDetails.getUsername(), pageable));
    }

    @PostMapping
    @Operation(summary = "Transfer money between two accounts")
    public ResponseEntity<TransactionDTO> transfer(@Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(request, userDetails.getUsername()));
    }
}
