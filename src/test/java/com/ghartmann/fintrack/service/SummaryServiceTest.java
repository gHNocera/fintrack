package com.ghartmann.fintrack.service;

import com.ghartmann.fintrack.domain.TransactionType;
import com.ghartmann.fintrack.dto.CategoryTotal;
import com.ghartmann.fintrack.dto.SummaryResponse;
import com.ghartmann.fintrack.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private SummaryService summaryService;

    @BeforeEach
    void setUp() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
    }

    @Test
    void getMonthlySummary_calculatesTotalsAndBalanceForTheWholeMonth() {
        LocalDate start = LocalDate.of(2026, 12, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        when(transactionRepository.sumByType(USER_ID, TransactionType.INCOME, start, end))
                .thenReturn(new BigDecimal("3000.00"));
        when(transactionRepository.sumByType(USER_ID, TransactionType.EXPENSE, start, end))
                .thenReturn(new BigDecimal("474.90"));
        when(transactionRepository.sumByCategory(USER_ID, TransactionType.EXPENSE, start, end))
                .thenReturn(List.of(
                        new CategoryTotal("Alimentacao", new BigDecimal("456.40")),
                        new CategoryTotal("Transporte", new BigDecimal("18.50"))));

        SummaryResponse summary = summaryService.getMonthlySummary(YearMonth.of(2026, 12));

        assertThat(summary.month()).isEqualTo("2026-12");
        assertThat(summary.totalIncome()).isEqualByComparingTo("3000.00");
        assertThat(summary.totalExpense()).isEqualByComparingTo("474.90");
        assertThat(summary.balance()).isEqualByComparingTo("2525.10");
        assertThat(summary.expensesByCategory()).hasSize(2);
        assertThat(summary.expensesByCategory().get(0).category()).isEqualTo("Alimentacao");
    }

    @Test
    void getMonthlySummary_whenThereAreNoTransactions_returnsZeros() {
        LocalDate start = LocalDate.of(2030, 1, 1);
        LocalDate end = LocalDate.of(2030, 1, 31);
        // O SUM do banco devolve null quando não há linhas
        when(transactionRepository.sumByType(USER_ID, TransactionType.INCOME, start, end)).thenReturn(null);
        when(transactionRepository.sumByType(USER_ID, TransactionType.EXPENSE, start, end)).thenReturn(null);
        when(transactionRepository.sumByCategory(USER_ID, TransactionType.EXPENSE, start, end))
                .thenReturn(List.of());

        SummaryResponse summary = summaryService.getMonthlySummary(YearMonth.of(2030, 1));

        assertThat(summary.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(summary.totalExpense()).isEqualByComparingTo("0.00");
        assertThat(summary.balance()).isEqualByComparingTo("0.00");
        assertThat(summary.expensesByCategory()).isEmpty();
    }

    @Test
    void getMonthlySummary_usesTheLastDayOfFebruaryInLeapYears() {
        LocalDate start = LocalDate.of(2028, 2, 1);
        LocalDate end = LocalDate.of(2028, 2, 29);
        when(transactionRepository.sumByType(USER_ID, TransactionType.INCOME, start, end))
                .thenReturn(new BigDecimal("100.00"));
        when(transactionRepository.sumByType(USER_ID, TransactionType.EXPENSE, start, end))
                .thenReturn(new BigDecimal("40.00"));

        SummaryResponse summary = summaryService.getMonthlySummary(YearMonth.of(2028, 2));

        assertThat(summary.balance()).isEqualByComparingTo("60.00");
    }
}