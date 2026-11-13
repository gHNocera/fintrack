package com.ghartmann.fintrack.service;

import com.ghartmann.fintrack.domain.User;
import com.ghartmann.fintrack.dto.AuthResponse;
import com.ghartmann.fintrack.dto.LoginRequest;
import com.ghartmann.fintrack.dto.RegisterRequest;
import com.ghartmann.fintrack.dto.UserResponse;
import com.ghartmann.fintrack.exception.ConflictException;
import com.ghartmann.fintrack.repository.UserRepository;
import com.ghartmann.fintrack.security.JwtService;
import com.ghartmann.fintrack.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_normalizesEmailAndStoresOnlyThePasswordHash() {
        when(userRepository.existsByEmail("gabriel@teste.com")).thenReturn(false);
        when(passwordEncoder.encode("senha12345")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        UserResponse response = authService.register(
                new RegisterRequest("  Gabriel  ", "  Gabriel@Teste.com ", "senha12345"));

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.name()).isEqualTo("Gabriel");
        assertThat(response.email()).isEqualTo("gabriel@teste.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("HASH");
        assertThat(captor.getValue().getPassword()).isNotEqualTo("senha12345");
    }

    @Test
    void register_whenEmailAlreadyExists_throwsConflictAndSavesNothing() {
        when(userRepository.existsByEmail("gabriel@teste.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Gabriel", "gabriel@teste.com", "senha12345")))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void login_returnsTheTokenForValidCredentials() {
        UserPrincipal principal = new UserPrincipal(5L, "gabriel@teste.com", "HASH");
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(jwtService.generateToken(principal)).thenReturn("token-123");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new LoginRequest("  Gabriel@Teste.com", "senha12345"));

        assertThat(response.accessToken()).isEqualTo("token-123");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600L);

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("gabriel@teste.com");
    }

    @Test
    void login_withWrongCredentials_propagatesTheErrorAndGeneratesNoToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("gabriel@teste.com", "errada")))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService);
    }
}