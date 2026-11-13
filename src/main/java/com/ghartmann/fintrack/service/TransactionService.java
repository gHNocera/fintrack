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
import com.ghartmann.fintrack.repository.TransactionSpecifications;
import com.ghartmann.fintrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final CurrentUserService currentUserService;
    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        Category category = getOwnedCategory(request.categoryId());
        User user = userRepository.findById(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Transaction transaction = new Transaction(
                request.description().trim(),
                request.amount(),
                request.type(),
                request.date(),
                category,
                user
        );

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> findAll(LocalDate from, LocalDate to, Long categoryId,
                                                     TransactionType type, int page, int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("A data inicial não pode ser posterior à data final");
        }

        Specification<Transaction> spec = TransactionSpecifications.belongsToUser(currentUserService.getCurrentUserId());
        if (from != null) {
            spec = spec.and(TransactionSpecifications.dateFrom(from));
        }
        if (to != null) {
            spec = spec.and(TransactionSpecifications.dateTo(to));
        }
        if (categoryId != null) {
            spec = spec.and(TransactionSpecifications.hasCategory(categoryId));
        }
        if (type != null) {
            spec = spec.and(TransactionSpecifications.hasType(type));
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Sort sort = Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        Page<TransactionResponse> result = transactionRepository.findAll(spec, pageable)
                .map(TransactionResponse::from);

        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id) {
        return TransactionResponse.from(getOwnedTransaction(id));
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction transaction = getOwnedTransaction(id);
        Category category = getOwnedCategory(request.categoryId());

        transaction.setDescription(request.description().trim());
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setDate(request.date());
        transaction.setCategory(category);

        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(Long id) {
        transactionRepository.delete(getOwnedTransaction(id));
    }

    private Transaction getOwnedTransaction(Long id) {
        return transactionRepository.findByIdAndUserId(id, currentUserService.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));
    }

    private Category getOwnedCategory(Long categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, currentUserService.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
    }
}