package com.ghartmann.fintrack.service;

import com.ghartmann.fintrack.domain.Category;
import com.ghartmann.fintrack.domain.User;
import com.ghartmann.fintrack.dto.CategoryRequest;
import com.ghartmann.fintrack.dto.CategoryResponse;
import com.ghartmann.fintrack.exception.ConflictException;
import com.ghartmann.fintrack.exception.ResourceNotFoundException;
import com.ghartmann.fintrack.repository.CategoryRepository;
import com.ghartmann.fintrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ghartmann.fintrack.repository.TransactionRepository;

import java.util.List;


@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        checkNameIsAvailable(name);

        User user = userRepository.findById(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Category saved = categoryRepository.save(new Category(name, user));
        return CategoryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByUserIdOrderByNameAsc(currentUserService.getCurrentUserId())
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(getOwnedCategory(id));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getOwnedCategory(id);
        String name = request.name().trim();

        if (!category.getName().equals(name)) {
            checkNameIsAvailable(name);
        }

        category.setName(name);
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getOwnedCategory(id);

        if (transactionRepository.existsByCategoryId(id)) {
            throw new ConflictException("Não é possível apagar: a categoria possui transações");
        }

        categoryRepository.delete(category);
    }

    private Category getOwnedCategory(Long id) {
        return categoryRepository.findByIdAndUserId(id, currentUserService.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
    }

    private void checkNameIsAvailable(String name) {
        if (categoryRepository.existsByNameAndUserId(name, currentUserService.getCurrentUserId())) {
            throw new ConflictException("Já existe uma categoria com o nome '" + name + "'");
        }
    }
}