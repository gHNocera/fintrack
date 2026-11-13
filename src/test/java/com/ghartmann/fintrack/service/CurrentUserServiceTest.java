package com.ghartmann.fintrack.service;

import com.ghartmann.fintrack.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserServiceTest {

    private final CurrentUserService currentUserService = new CurrentUserService();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_returnsTheIdOfTheAuthenticatedUser() {
        UserPrincipal principal = new UserPrincipal(42L, "gabriel@teste.com", "hash");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        assertThat(currentUserService.getCurrentUserId()).isEqualTo(42L);
    }

    @Test
    void getCurrentUserId_withoutAuthentication_throwsIllegalState() {
        assertThatThrownBy(() -> currentUserService.getCurrentUserId())
                .isInstanceOf(IllegalStateException.class);
    }
}