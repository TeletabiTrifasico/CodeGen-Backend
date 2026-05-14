package com.banking.controller;

import com.banking.dto.account.AccountDTO;
import com.banking.dto.account.CreateAccountRequest;
import com.banking.dto.account.UpdateAbsoluteLimitRequest;
import com.banking.dto.account.UpdateDailyLimitRequest;
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
    @Operation(summary = "Get all customer accounts or accounts for a specific user (employee only)")
    public ResponseEntity<List<AccountDTO>> getAccounts(
            @RequestParam(required = false) Long userId) {

        if (userId != null) {
            return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
        }
        return ResponseEntity.ok(accountService.getAllCustomerAccounts());
    }

    @GetMapping("/myAccounts")
    @Operation(summary = "Get all accounts for the authenticated user")
    public ResponseEntity<List<AccountDTO>> getMyAccounts(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(accountService.getAccountsForCurrentUser(userDetails.getUsername()));
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

    @PutMapping("/{iban}/close")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Close a customer account (employee only)")
    public ResponseEntity<AccountDTO> closeAccount(@PathVariable String iban) {
        return ResponseEntity.ok(accountService.closeAccount(iban));
    }

    @PutMapping("/{iban}/absolute-limit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Update the absolute limit of an account (employee only)")
    public ResponseEntity<AccountDTO> updateAbsoluteLimit(@PathVariable String iban,
                                                          @Valid @RequestBody UpdateAbsoluteLimitRequest request) {
        return ResponseEntity.ok(accountService.updateAbsoluteLimit(iban, request));
    }

    @PutMapping("/{iban}/daily-limit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Update the daily limit of an account (employee only)")
    public ResponseEntity<AccountDTO> updateDailyLimit(@PathVariable String iban,
                                                       @Valid @RequestBody UpdateDailyLimitRequest request) {
        return ResponseEntity.ok(accountService.updateDailyLimit(iban, request));
    }


}
