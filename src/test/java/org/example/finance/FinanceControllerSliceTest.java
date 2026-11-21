package org.example.finance;

import org.example.auth.service.JwtAuthenticationFilter;
import org.example.finance.model.FinancialAccount;
import org.example.finance.repo.TransactionRepository;
import org.example.finance.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(org.example.finance.api.FinanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class FinanceControllerSliceTest {
    @Autowired MockMvc mvc;
    @MockBean AccountService accounts;
    @MockBean CategoryService categories;
    @MockBean TransactionService transactions;
    @MockBean BudgetService budgets;
    @MockBean SavingsGoalService goals;
    @MockBean NotificationService notifications;
    @MockBean AnalyticsService analytics;
    @MockBean RecurringTransactionService recurring;
    @MockBean TransactionRepository transactionRepository;
    @MockBean JwtAuthenticationFilter jwtFilter;

    @Test void accountCreationUsesPrincipalAndReturnsCreatedResponse() throws Exception {
        when(accounts.create("alice", "Cash", "CASH", "INR", new BigDecimal("25.00")))
                .thenReturn(new FinancialAccount("alice", "Cash", "CASH", "INR", new BigDecimal("25.00")));
        mvc.perform(post("/api/accounts").principal(() -> "alice").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cash\",\"type\":\"CASH\",\"currency\":\"INR\",\"openingBalance\":25.00}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Cash"))
                .andExpect(jsonPath("$.balance").value(25));
        verify(accounts).create("alice", "Cash", "CASH", "INR", new BigDecimal("25.00"));
        mvc.perform(post("/api/accounts").principal(() -> "alice").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"type\":\"CASH\",\"currency\":\"INR\",\"openingBalance\":25.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test void accountListingMapsDomainObjectsToResponses() throws Exception {
        when(accounts.list("alice")).thenReturn(List.of(new FinancialAccount("alice", "Wallet", "CASH", "INR", BigDecimal.TEN)));
        mvc.perform(get("/api/accounts").principal(() -> "alice"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Wallet"));
    }
}
