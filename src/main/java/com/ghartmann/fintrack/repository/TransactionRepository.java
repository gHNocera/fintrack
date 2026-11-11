package com.ghartmann.fintrack.repository;

import com.ghartmann.fintrack.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @EntityGraph(attributePaths = "category")
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);
}