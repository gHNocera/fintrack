package com.ghartmann.fintrack.repository;

import com.ghartmann.fintrack.domain.Transaction;
import com.ghartmann.fintrack.domain.TransactionType;
import com.ghartmann.fintrack.dto.CategoryTotal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @EntityGraph(attributePaths = "category")
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    @Query("""
            select sum(t.amount)
            from Transaction t
            where t.user.id = :userId
              and t.type = :type
              and t.date between :start and :end
            """)
    BigDecimal sumByType(@Param("userId") Long userId,
                         @Param("type") TransactionType type,
                         @Param("start") LocalDate start,
                         @Param("end") LocalDate end);

    @Query("""
            select new com.ghartmann.fintrack.dto.CategoryTotal(c.name, sum(t.amount))
            from Transaction t
            join t.category c
            where t.user.id = :userId
              and t.type = :type
              and t.date between :start and :end
            group by c.id, c.name
            order by sum(t.amount) desc
            """)
    List<CategoryTotal> sumByCategory(@Param("userId") Long userId,
                                      @Param("type") TransactionType type,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end);
}