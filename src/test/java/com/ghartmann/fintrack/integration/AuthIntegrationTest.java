package com.ghartmann.fintrack.integration;

import com.ghartmann.fintrack.security.JwtService;
import com.ghartmann.fintrack.security.UserPrincipal;
import com.ghartmann.fintrack.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private JwtService jwtService;

    @Test
    void register_createsTheUserAndNeverReturnsThePassword() throws Exception {
        post("/auth/register", null, registerJson("Gabriel", uniqueEmail(), PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Gabriel"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_withEmailAlreadyInUse_returns409EvenWithDifferentCase() throws Exception {
        String email = uniqueEmail();
        post("/auth/register", null, registerJson("Gabriel", email, PASSWORD))
                .andExpect(status().isCreated());

        post("/auth/register", null, registerJson("Outro", email.toUpperCase(Locale.ROOT), PASSWORD))
                .andExpect(status().isConflict());
    }

    @Test
    void register_withInvalidData_returns400WithFieldErrors() throws Exception {
        post("/auth/register", null, registerJson("", "naoeumemail", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void login_withValidCredentials_returnsABearerToken() throws Exception {
        String email = uniqueEmail();
        post("/auth/register", null, registerJson("Gabriel", email, PASSWORD))
                .andExpect(status().isCreated());

        post("/auth/login", null, loginJson(email, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        String email = uniqueEmail();
        post("/auth/register", null, registerJson("Gabriel", email, PASSWORD))
                .andExpect(status().isCreated());

        post("/auth/login", null, loginJson(email, "senhaerrada1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void login_withUnknownEmail_returns401() throws Exception {
        post("/auth/login", null, loginJson(uniqueEmail(), PASSWORD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withoutToken_returns401() throws Exception {
        get("/categories", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedRoute_withInvalidToken_returns401() throws Exception {
        get("/categories", "abc.def.ghi")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withValidToken_returns200() throws Exception {
        String token = registerAndLogin();

        get("/categories", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void protectedRoute_withValidTokenOfAUserThatNoLongerExists_returns401() throws Exception {
        String token = jwtService.generateToken(new UserPrincipal(999999L, "fantasma@teste.com", "x"));

        get("/categories", token)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withTokenForgedWithAnotherKey_returns401() throws Exception {
        String email = uniqueEmail();
        post("/auth/register", null, registerJson("Gabriel", email, PASSWORD))
                .andExpect(status().isCreated());

        byte[] otherKey = new byte[32];
        new SecureRandom().nextBytes(otherKey);
        JwtService attackerService = new JwtService(Base64.getEncoder().encodeToString(otherKey), 60);
        String forgedToken = attackerService.generateToken(new UserPrincipal(1L, email, "x"));

        get("/categories", forgedToken)
                .andExpect(status().isUnauthorized());
    }
}