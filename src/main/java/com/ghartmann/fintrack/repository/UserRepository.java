package com.ghartmann.fintrack.repository;

import com.ghartmann.fintrack.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}