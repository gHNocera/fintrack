package com.ghartmann.fintrack.dto;

import com.ghartmann.fintrack.domain.Transaction;
import com.ghartmann.fintrack.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        String description,
        BigDecimal amount,
        TransactionType type,
        LocalDate date,
        Long categoryId,
        String categoryName
) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getDate(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName()
        );
    }
}