package com.banking.controller;

import com.banking.dto.user.UserDTO;
import com.banking.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management (employee only)")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get all users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("without-accounts")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get customers who do not have accounts yet")
    public ResponseEntity<List<UserDTO>> getCustomersWithoutAccounts() {
        return ResponseEntity.ok(userService.getCustomersWithoutAccounts());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get customers awaiting approval")
    public ResponseEntity<List<UserDTO>> getPendingCustomers() {
        return ResponseEntity.ok(userService.getPendingCustomers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Approve a customer account and create their checking and saving accounts")
    public ResponseEntity<UserDTO> approveUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.approveUser(id));
    }
}
