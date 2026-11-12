package com.ghartmann.fintrack.service;

import com.ghartmann.fintrack.domain.TransactionType;
import com.ghartmann.fintrack.dto.CategoryTotal;
import com.ghartmann.fintrack.dto.SummaryResponse;
import com.ghartmann.fintrack.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SummaryService {

    // TEMPORÁRIO: na Fase 3 isso será substituído pelo usuário logado (JWT).
    private static final Long TEMP_USER_ID = 1L;

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public SummaryResponse getMonthlySummary(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        BigDecimal totalIncome = orZero(
                transactionRepository.sumByType(TEMP_USER_ID, TransactionType.INCOME, start, end));
        BigDecimal totalExpense = orZero(
                transactionRepository.sumByType(TEMP_USER_ID, TransactionType.EXPENSE, start, end));
        BigDecimal balance = totalIncome.subtract(totalExpense);

        List<CategoryTotal> expensesByCategory =
                transactionRepository.sumByCategory(TEMP_USER_ID, TransactionType.EXPENSE, start, end);

        return new SummaryResponse(month.toString(), totalIncome, totalExpense, balance, expensesByCategory);
    }

    // O SUM devolve null quando não há linhas; aqui transformamos em 0,00
    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value;
    }
}