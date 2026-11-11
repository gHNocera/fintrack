package com.ghartmann.fintrack.dto;

import com.ghartmann.fintrack.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres")
        String description,

        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "O valor deve ter no máximo 10 dígitos e 2 casas decimais")
        BigDecimal amount,

        @NotNull(message = "O tipo é obrigatório (INCOME ou EXPENSE)")
        TransactionType type,

        @NotNull(message = "A data é obrigatória")
        LocalDate date,

        @NotNull(message = "A categoria é obrigatória")
        Long categoryId
) {
}