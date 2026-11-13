package com.ghartmann.fintrack.support;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    protected static final String PASSWORD = "senha12345";

    @Autowired
    private WebApplicationContext context;

    protected MockMvc mockMvc;

    // Nos testes, a chave do JWT é gerada na hora: não depende da variável JWT_SECRET
    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        String secret = Base64.getEncoder().encodeToString(key);
        registry.add("app.jwt.secret", () -> secret);
    }

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ---------- Requisições ----------

    protected ResultActions get(String url, String token) throws Exception {
        return perform(MockMvcRequestBuilders.get(url), token);
    }

    protected ResultActions post(String url, String token, String json) throws Exception {
        return perform(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    protected ResultActions put(String url, String token, String json) throws Exception {
        return perform(MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    protected ResultActions delete(String url, String token) throws Exception {
        return perform(MockMvcRequestBuilders.delete(url), token);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String token) throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    // ---------- JSON ----------

    protected static String registerJson(String name, String email, String password) {
        return """
                {"name":"%s","email":"%s","password":"%s"}
                """.formatted(name, email, password);
    }

    protected static String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    protected static String categoryJson(String name) {
        return """
                {"name":"%s"}
                """.formatted(name);
    }

    protected static String transactionJson(String description, String amount, String type,
                                            String date, long categoryId) {
        return """
                {"description":"%s","amount":%s,"type":"%s","date":"%s","categoryId":%d}
                """.formatted(description, amount, type, date, categoryId);
    }

    // ---------- Atalhos para montar cenários ----------

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@teste.com";
    }

    protected String registerAndLogin() throws Exception {
        String email = uniqueEmail();
        post("/auth/register", null, registerJson("Usuario Teste", email, PASSWORD))
                .andExpect(status().isCreated());

        String body = post("/auth/login", null, loginJson(email, PASSWORD))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.accessToken");
    }

    protected long createCategory(String token, String name) throws Exception {
        String body = post("/categories", token, categoryJson(name))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.<Number>read(body, "$.id").longValue();
    }

    protected long createTransaction(String token, String description, String amount, String type,
                                     String date, long categoryId) throws Exception {
        String body = post("/transactions", token, transactionJson(description, amount, type, date, categoryId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.<Number>read(body, "$.id").longValue();
    }
}