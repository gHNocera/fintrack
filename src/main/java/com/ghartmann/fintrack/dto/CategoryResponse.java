package com.ghartmann.fintrack.dto;

import com.ghartmann.fintrack.domain.Category;

public record CategoryResponse(Long id, String name) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}