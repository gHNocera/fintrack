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

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    // TEMPORÁRIO
    private static final Long TEMP_USER_ID = 1L;

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        checkNameIsAvailable(name);

        User user = userRepository.findById(TEMP_USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Category saved = categoryRepository.save(new Category(name, user));
        return CategoryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByUserIdOrderByNameAsc(TEMP_USER_ID)
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
        categoryRepository.delete(getOwnedCategory(id));
    }

    private Category getOwnedCategory(Long id) {
        return categoryRepository.findByIdAndUserId(id, TEMP_USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
    }

    private void checkNameIsAvailable(String name) {
        if (categoryRepository.existsByNameAndUserId(name, TEMP_USER_ID)) {
            throw new ConflictException("Já existe uma categoria com o nome '" + name + "'");
        }
    }
}