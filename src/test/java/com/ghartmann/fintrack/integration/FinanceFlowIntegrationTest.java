package com.ghartmann.fintrack.integration;

import com.ghartmann.fintrack.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FinanceFlowIntegrationTest extends IntegrationTestSupport {

    // ---------- Categorias ----------

    @Test
    void categories_fullLifecycle() throws Exception {
        String token = registerAndLogin();
        long id = createCategory(token, "Mercado");

        post("/categories", token, categoryJson("Mercado"))
                .andExpect(status().isConflict());

        get("/categories", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Mercado"));

        put("/categories/" + id, token, categoryJson("Supermercado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Supermercado"));

        delete("/categories/" + id, token)
                .andExpect(status().isNoContent());

        get("/categories/" + id, token)
                .andExpect(status().isNotFound());
    }

    @Test
    void category_withBlankName_returns400WithFieldErrors() throws Exception {
        String token = registerAndLogin();

        post("/categories", token, categoryJson(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    // ---------- Transações ----------

    @Test
    void transaction_create_returnsTheTransactionWithItsCategoryName() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Alimentacao");

        post("/transactions", token, transactionJson("Almoco", "35.50", "EXPENSE", "2026-12-10", food))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("Almoco"))
                .andExpect(jsonPath("$.amount").value(35.50))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.date").value("2026-12-10"))
                .andExpect(jsonPath("$.categoryId").value(food))
                .andExpect(jsonPath("$.categoryName").value("Alimentacao"));
    }

    @Test
    void transaction_withInvalidAmount_returns400WithFieldErrors() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Alimentacao");

        post("/transactions", token, transactionJson("Almoco", "0", "EXPENSE", "2026-12-10", food))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void transaction_withUnknownType_returns400() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Alimentacao");

        post("/transactions", token, transactionJson("Almoco", "35.50", "FOO", "2026-12-10", food))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transaction_withUnknownCategory_returns404() throws Exception {
        String token = registerAndLogin();

        post("/transactions", token, transactionJson("Almoco", "35.50", "EXPENSE", "2026-12-10", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void transactions_canBeFilteredByTypePeriodAndCategory() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Alimentacao");
        long salary = createCategory(token, "Salario");
        createTransaction(token, "Almoco", "35.50", "EXPENSE", "2026-12-10", food);
        createTransaction(token, "Mercado", "420.90", "EXPENSE", "2026-11-28", food);
        createTransaction(token, "Pagamento", "3000.00", "INCOME", "2026-12-05", salary);

        get("/transactions?type=EXPENSE", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        get("/transactions?from=2026-12-01&to=2026-12-31", token)
                .andExpect(jsonPath("$.totalElements").value(2));

        get("/transactions?categoryId=" + salary, token)
                .andExpect(jsonPath("$.totalElements").value(1));

        get("/transactions?type=EXPENSE&from=2026-12-01", token)
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void transactions_arePaginatedAndSortedFromNewestToOldest() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Alimentacao");
        createTransaction(token, "A", "10.00", "EXPENSE", "2026-12-01", food);
        createTransaction(token, "B", "20.00", "EXPENSE", "2026-12-10", food);
        createTransaction(token, "C", "30.00", "EXPENSE", "2026-12-20", food);

        get("/transactions?page=0&size=2", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].date").value("2026-12-20"))
                .andExpect(jsonPath("$.content[1].date").value("2026-12-10"));

        get("/transactions?page=1&size=2", token)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].date").value("2026-12-01"));
    }

    @Test
    void transactions_withStartDateAfterEndDate_returns400() throws Exception {
        String token = registerAndLogin();

        get("/transactions?from=2026-12-01&to=2026-11-01", token)
                .andExpect(status().isBadRequest());
    }

    @Test
    void transaction_canBeUpdatedAndDeleted() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Alimentacao");
        long salary = createCategory(token, "Salario");
        long id = createTransaction(token, "Almoco", "35.50", "EXPENSE", "2026-12-10", food);

        put("/transactions/" + id, token,
                transactionJson("Pagamento", "3000.00", "INCOME", "2026-12-05", salary))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Pagamento"))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.categoryName").value("Salario"));

        delete("/transactions/" + id, token)
                .andExpect(status().isNoContent());

        get("/transactions/" + id, token)
                .andExpect(status().isNotFound());
    }

    @Test
    void category_thatHasTransactions_cannotBeDeleted() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Alimentacao");
        createTransaction(token, "Almoco", "35.50", "EXPENSE", "2026-12-10", food);

        delete("/categories/" + food, token)
                .andExpect(status().isConflict());
    }

    // ---------- Resumo mensal ----------

    @Test
    void summary_calculatesTheMonthAndIgnoresTransactionsOutsideIt() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Alimentacao");
        long transport = createCategory(token, "Transporte");
        long salary = createCategory(token, "Salario");

        createTransaction(token, "Salario", "3000.00", "INCOME", "2026-12-01", salary);
        createTransaction(token, "Almoco", "35.50", "EXPENSE", "2026-12-10", food);
        createTransaction(token, "Mercado", "420.90", "EXPENSE", "2026-12-15", food);
        createTransaction(token, "Uber", "18.50", "EXPENSE", "2026-12-31", transport);
        // Fora do mês: nao podem entrar no resumo de dezembro
        createTransaction(token, "Fora-Novembro", "999.00", "EXPENSE", "2026-11-30", food);
        createTransaction(token, "Fora-Janeiro", "999.00", "EXPENSE", "2027-01-01", transport);

        get("/summary?month=2026-12", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-12"))
                .andExpect(jsonPath("$.totalIncome").value(3000.00))
                .andExpect(jsonPath("$.totalExpense").value(474.90))
                .andExpect(jsonPath("$.balance").value(2525.10))
                .andExpect(jsonPath("$.expensesByCategory.length()").value(2))
                .andExpect(jsonPath("$.expensesByCategory[0].category").value("Alimentacao"))
                .andExpect(jsonPath("$.expensesByCategory[0].total").value(456.40))
                .andExpect(jsonPath("$.expensesByCategory[1].category").value("Transporte"))
                .andExpect(jsonPath("$.expensesByCategory[1].total").value(18.50));
    }

    @Test
    void summary_forAMonthWithoutData_returnsZeros() throws Exception {
        String token = registerAndLogin();

        get("/summary?month=2030-01", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0.0))
                .andExpect(jsonPath("$.totalExpense").value(0.0))
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.expensesByCategory").isEmpty());
    }

    @Test
    void summary_withInvalidMonth_returns400() throws Exception {
        String token = registerAndLogin();

        get("/summary?month=2026-13", token).andExpect(status().isBadRequest());
        get("/summary?month=dezembro", token).andExpect(status().isBadRequest());
    }
}