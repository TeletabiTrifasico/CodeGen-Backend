package com.banking.dto.atm;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AtmResponse {
    private String reference;
    private BigDecimal newBalance;
    private String message;
}
