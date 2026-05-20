package com.banking.controller;
import com.banking.dto.account.AccountDTO;
import com.banking.dto.account.CreateAccountRequest;
import com.banking.dto.account.UpdateAccountRequest;
import com.banking.service.AccountService;
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
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Bank account operations")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get all customer accounts or accounts of specific user by User ID - Employee only")
    public ResponseEntity<List<AccountDTO>> getAccounts(
            @RequestParam(required = false) Long userId) {
        if (userId != null) {
            return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
        }
        return ResponseEntity.ok(accountService.getAllCustomerAccounts());
    }

    @GetMapping("/accounts")
    @Operation(summary = "Get all accounts of an authenticated user")
    public ResponseEntity<List<AccountDTO>> getMyAccounts(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(accountService.getAccountsOfCurrentUser(userDetails.getUsername()));
    }

    @GetMapping("/{iban}")
    @Operation(summary = "Get account details by IBAN")
    public ResponseEntity<AccountDTO> getByIban(@PathVariable String iban) {
        return ResponseEntity.ok(accountService.getAccountByIban(iban));
    }

    @PostMapping
    @Operation(summary = "Create a new account")
    public ResponseEntity<AccountDTO> createAccount(@Valid @RequestBody CreateAccountRequest request,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request, userDetails.getUsername()));
    }

    @PatchMapping("/{iban}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary ="Update an account - close,daily limit, transaction limit, - Employee only")
    public ResponseEntity<AccountDTO> updateAccount(@PathVariable String iban,
                                                    @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(iban, request));
    }


}
