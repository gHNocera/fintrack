package com.ghartmann.fintrack.integration;

import com.ghartmann.fintrack.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserIsolationIntegrationTest extends IntegrationTestSupport {

    private record OwnerData(String token, long categoryId, long transactionId) {
    }

    private OwnerData createOwnerWithData() throws Exception {
        String token = registerAndLogin();
        long categoryId = createCategory(token, "Alimentacao");
        long transactionId = createTransaction(token, "Almoco", "35.50", "EXPENSE", "2026-12-10", categoryId);
        return new OwnerData(token, categoryId, transactionId);
    }

    @Test
    void anotherUser_seesEmptyLists() throws Exception {
        createOwnerWithData();
        String intruder = registerAndLogin();

        get("/categories", intruder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        get("/transactions", intruder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void anotherUser_cannotReadByIdAndGets404() throws Exception {
        OwnerData owner = createOwnerWithData();
        String intruder = registerAndLogin();

        get("/categories/" + owner.categoryId(), intruder).andExpect(status().isNotFound());
        get("/transactions/" + owner.transactionId(), intruder).andExpect(status().isNotFound());
    }

    @Test
    void anotherUser_cannotChangeOrDelete_andTheOwnersDataStaysIntact() throws Exception {
        OwnerData owner = createOwnerWithData();
        String intruder = registerAndLogin();

        put("/categories/" + owner.categoryId(), intruder, categoryJson("Invadida"))
                .andExpect(status().isNotFound());
        delete("/categories/" + owner.categoryId(), intruder).andExpect(status().isNotFound());
        delete("/transactions/" + owner.transactionId(), intruder).andExpect(status().isNotFound());

        get("/categories/" + owner.categoryId(), owner.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alimentacao"));
        get("/transactions/" + owner.transactionId(), owner.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Almoco"));
    }

    @Test
    void anotherUser_cannotCreateATransactionInSomeoneElsesCategory() throws Exception {
        OwnerData owner = createOwnerWithData();
        String intruder = registerAndLogin();

        post("/transactions", intruder,
                transactionJson("Invasao", "10.00", "EXPENSE", "2026-12-10", owner.categoryId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void summaries_areIndependentPerUser() throws Exception {
        OwnerData owner = createOwnerWithData();
        String intruder = registerAndLogin();

        get("/summary?month=2026-12", intruder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(0.0));

        get("/summary?month=2026-12", owner.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(35.50));
    }
}