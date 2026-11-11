package com.ghartmann.fintrack.repository;

import com.ghartmann.fintrack.domain.Transaction;
import com.ghartmann.fintrack.domain.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> belongsToUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Transaction> dateFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDate>get("date"), from);
    }

    public static Specification<Transaction> dateTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<LocalDate>get("date"), to);
    }

    public static Specification<Transaction> hasCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Transaction> hasType(TransactionType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }
}