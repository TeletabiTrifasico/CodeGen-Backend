package com.banking.service;

import com.banking.dto.atm.AtmRequest;
import com.banking.dto.atm.AtmResponse;

public interface AtmService {
    AtmResponse deposit(AtmRequest request, String currentUsername);
    AtmResponse withdraw(AtmRequest request, String currentUsername);
}
