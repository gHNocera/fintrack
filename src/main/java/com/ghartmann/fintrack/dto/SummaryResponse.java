package com.ghartmann.fintrack.dto;

import java.math.BigDecimal;
import java.util.List;

public record SummaryResponse(
        String month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        List<CategoryTotal> expensesByCategory
) {
}