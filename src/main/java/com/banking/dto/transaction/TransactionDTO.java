package com.banking.dto.transaction;

import com.banking.entity.Transaction;
import com.banking.enums.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionDTO {
    private Long id;
    private String reference;
    private String sourceIban;
    private String destinationIban;
    private BigDecimal amount;
    private String description;
    private TransactionType type;
    private LocalDateTime timestamp;
    private String initiatedByUsername;

    public static TransactionDTO from(Transaction t) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(t.getId());
        dto.setReference(t.getReference());
        dto.setSourceIban(t.getSourceAccount() != null ? t.getSourceAccount().getIban() : null);
        dto.setDestinationIban(t.getDestinationAccount() != null ? t.getDestinationAccount().getIban() : null);
        dto.setAmount(t.getAmount());
        dto.setDescription(t.getDescription());
        dto.setType(t.getType());
        dto.setTimestamp(t.getTimestamp());
        dto.setInitiatedByUsername(t.getInitiatedBy() != null ? t.getInitiatedBy().getUsername() : null);
        return dto;
    }
}
