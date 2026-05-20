package com.banking.dto.account;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UpdateAccountRequest
{
    private BigDecimal absoluteLimit;
    private BigDecimal dayLimit;
    private BigDecimal transactionLimit;
    private boolean active;

}

