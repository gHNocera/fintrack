package com.ghartmann.fintrack.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final UserPrincipal USER = new UserPrincipal(1L, "gabriel@teste.com", "hash");

    @Test
    void generatedToken_canBeReadBack() {
        JwtService service = new JwtService(randomSecret(), 60);

        String token = service.generateToken(USER);

        assertThat(service.extractEmail(token)).isEqualTo("gabriel@teste.com");
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        String token = new JwtService(randomSecret(), 60).generateToken(USER);
        JwtService otherService = new JwtService(randomSecret(), 60);

        assertThatThrownBy(() -> otherService.extractEmail(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenWithTamperedPayload_isRejected() {
        JwtService service = new JwtService(randomSecret(), 60);
        String[] parts = service.generateToken(USER).split("\\.");
        String fakePayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"hacker@teste.com\"}".getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + fakePayload + "." + parts[2];

        assertThatThrownBy(() -> service.extractEmail(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void expiredToken_isRejected() {
        JwtService service = new JwtService(randomSecret(), -1);
        String token = service.generateToken(USER);

        assertThatThrownBy(() -> service.extractEmail(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void garbageToken_isRejected() {
        JwtService service = new JwtService(randomSecret(), 60);

        assertThatThrownBy(() -> service.extractEmail("abc.def.ghi"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void expirationInSeconds_isTheConfiguredMinutesTimesSixty() {
        assertThat(new JwtService(randomSecret(), 60).getExpirationSeconds()).isEqualTo(3600);
    }

    private static String randomSecret() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}