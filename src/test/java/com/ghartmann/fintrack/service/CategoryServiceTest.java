package com.ghartmann.fintrack.service;

import com.ghartmann.fintrack.domain.Category;
import com.ghartmann.fintrack.domain.User;
import com.ghartmann.fintrack.dto.CategoryRequest;
import com.ghartmann.fintrack.dto.CategoryResponse;
import com.ghartmann.fintrack.exception.ConflictException;
import com.ghartmann.fintrack.exception.ResourceNotFoundException;
import com.ghartmann.fintrack.repository.CategoryRepository;
import com.ghartmann.fintrack.repository.TransactionRepository;
import com.ghartmann.fintrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
    }

    @Test
    void create_savesCategoryWithTrimmedName() {
        User user = user();
        when(categoryRepository.existsByNameAndUserId("Mercado", USER_ID)).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        CategoryResponse response = categoryService.create(new CategoryRequest("  Mercado  "));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Mercado");
    }

    @Test
    void create_whenNameAlreadyExists_throwsConflict() {
        when(categoryRepository.existsByNameAndUserId("Mercado", USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CategoryRequest("Mercado")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Mercado");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void findById_whenCategoryIsNotOwnedByUser_throwsNotFound() {
        when(categoryRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_changesTheName() {
        Category category = category(1L, "Antigo");
        when(categoryRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndUserId("Novo", USER_ID)).thenReturn(false);

        CategoryResponse response = categoryService.update(1L, new CategoryRequest("Novo"));

        assertThat(response.name()).isEqualTo("Novo");
        assertThat(category.getName()).isEqualTo("Novo");
    }

    @Test
    void update_toNameUsedByAnotherCategory_throwsConflict() {
        Category category = category(1L, "Antigo");
        when(categoryRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndUserId("Outra", USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(1L, new CategoryRequest("Outra")))
                .isInstanceOf(ConflictException.class);

        assertThat(category.getName()).isEqualTo("Antigo");
    }

    @Test
    void update_keepingTheSameName_doesNotCheckDuplicates() {
        Category category = category(1L, "Mercado");
        when(categoryRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.update(1L, new CategoryRequest("Mercado"));

        assertThat(response.name()).isEqualTo("Mercado");
        verify(categoryRepository, never()).existsByNameAndUserId(any(), any());
    }

    @Test
    void delete_whenCategoryHasTransactions_throwsConflict() {
        Category category = category(1L, "Mercado");
        when(categoryRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(ConflictException.class);

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void delete_removesTheCategory() {
        Category category = category(1L, "Mercado");
        when(categoryRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.delete(1L);

        verify(categoryRepository).delete(category);
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