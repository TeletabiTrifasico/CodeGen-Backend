package com.banking.dto.account;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateDailyLimitRequest {
    @NotNull
    private BigDecimal dayLimit;
}
