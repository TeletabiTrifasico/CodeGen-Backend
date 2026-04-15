package com.banking.controller;

import com.banking.dto.atm.AtmRequest;
import com.banking.dto.atm.AtmResponse;
import com.banking.service.AtmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atm")
@Tag(name = "ATM", description = "ATM deposit and withdrawal operations")
@RequiredArgsConstructor
public class AtmController {

    private final AtmService atmService;

    @PostMapping("/deposit")
    @Operation(summary = "Deposit cash into an account via ATM")
    public ResponseEntity<AtmResponse> deposit(@Valid @RequestBody AtmRequest request,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(atmService.deposit(request, userDetails.getUsername()));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw cash from an account via ATM")
    public ResponseEntity<AtmResponse> withdraw(@Valid @RequestBody AtmRequest request,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(atmService.withdraw(request, userDetails.getUsername()));
    }
}
