package com.ghartmann.fintrack.service;

import com.ghartmann.fintrack.domain.Category;
import com.ghartmann.fintrack.domain.Transaction;
import com.ghartmann.fintrack.domain.TransactionType;
import com.ghartmann.fintrack.domain.User;
import com.ghartmann.fintrack.dto.PageResponse;
import com.ghartmann.fintrack.dto.TransactionRequest;
import com.ghartmann.fintrack.dto.TransactionResponse;
import com.ghartmann.fintrack.exception.BadRequestException;
import com.ghartmann.fintrack.exception.ResourceNotFoundException;
import com.ghartmann.fintrack.repository.CategoryRepository;
import com.ghartmann.fintrack.repository.TransactionRepository;
import com.ghartmann.fintrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
    }

    @Test
    void create_savesTransactionLinkedToTheUsersCategory() {
        Category category = category(2L, "Alimentacao");
        when(categoryRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(category));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        TransactionRequest request = new TransactionRequest(
                "  Almoco  ", new BigDecimal("35.50"), TransactionType.EXPENSE, LocalDate.of(2026, 12, 10), 2L);

        TransactionResponse response = transactionService.create(request);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.description()).isEqualTo("Almoco");
        assertThat(response.amount()).isEqualByComparingTo("35.50");
        assertThat(response.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 12, 10));
        assertThat(response.categoryId()).isEqualTo(2L);
        assertThat(response.categoryName()).isEqualTo("Alimentacao");
    }

    @Test
    void create_whenCategoryDoesNotBelongToUser_throwsNotFoundAndSavesNothing() {
        when(categoryRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        TransactionRequest request = new TransactionRequest(
                "Almoco", new BigDecimal("35.50"), TransactionType.EXPENSE, LocalDate.of(2026, 12, 10), 99L);

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).save(any(Transaction.class));
        verifyNoInteractions(userRepository);
    }

    @Test
    void findAll_whenStartDateIsAfterEndDate_throwsBadRequest() {
        LocalDate from = LocalDate.of(2026, 12, 1);
        LocalDate to = LocalDate.of(2026, 11, 1);

        assertThatThrownBy(() -> transactionService.findAll(from, to, null, null, 0, 20))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void findAll_normalizesPageAndSize() {
        when(transactionRepository.findAll(anySpec(), any(Pageable.class))).thenReturn(Page.empty());

        transactionService.findAll(null, null, null, null, -5, 5000);

        Pageable pageable = capturePageable();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(100);
    }

    @Test
    void findAll_sortsByDateThenIdDescending() {
        when(transactionRepository.findAll(anySpec(), any(Pageable.class))).thenReturn(Page.empty());

        transactionService.findAll(null, null, null, null, 0, 20);

        Sort expected = Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "id"));
        assertThat(capturePageable().getSort()).isEqualTo(expected);
    }

    @Test
    void findAll_mapsThePageToTheResponse() {
        Transaction transaction = new Transaction("Almoco", new BigDecimal("35.50"), TransactionType.EXPENSE,
                LocalDate.of(2026, 12, 10), category(2L, "Alimentacao"), user());
        transaction.setId(7L);
        Page<Transaction> page = new PageImpl<>(List.of(transaction), PageRequest.of(0, 20), 1);
        when(transactionRepository.findAll(anySpec(), any(Pageable.class))).thenReturn(page);

        PageResponse<TransactionResponse> result = transactionService.findAll(null, null, null, null, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).categoryName()).isEqualTo("Alimentacao");
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
    }

    @Test
    void update_changesAllFieldsAndTheCategory() {
        Transaction existing = new Transaction("Velha", new BigDecimal("10.00"), TransactionType.EXPENSE,
                LocalDate.of(2026, 1, 1), category(1L, "Antiga"), user());
        existing.setId(5L);
        Category newCategory = category(2L, "Nova");
        when(transactionRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(newCategory));

        TransactionRequest request = new TransactionRequest(
                " Nova descricao ", new BigDecimal("99.90"), TransactionType.INCOME, LocalDate.of(2026, 12, 25), 2L);

        TransactionResponse response = transactionService.update(5L, request);

        assertThat(response.description()).isEqualTo("Nova descricao");
        assertThat(response.amount()).isEqualByComparingTo("99.90");
        assertThat(response.type()).isEqualTo(TransactionType.INCOME);
        assertThat(response.categoryName()).isEqualTo("Nova");
        assertThat(existing.getCategory()).isSameAs(newCategory);
    }

    @Test
    void delete_whenTransactionDoesNotBelongToUser_throwsNotFound() {
        when(transactionRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(5L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    // Matcher auxiliar: "qualquer Specification"
    private static Specification<Transaction> anySpec() {
        return ArgumentMatchers.any();
    }

    private Pageable capturePageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findAll(anySpec(), captor.capture());
        return captor.getValue();
    }

    private User user() {
        User user = new User();
        user.setId(USER_ID);
        return user;
    }

    private Category category(Long id, String name) {
        Category category = new Category(name, user());
        category.setId(id);
        return category;
    }
}