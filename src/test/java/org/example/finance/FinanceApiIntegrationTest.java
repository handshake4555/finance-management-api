package org.example.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FinanceApiIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    private String register(String name) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"" + name + "\",\"password\":\"strong-pass-123\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }

    @Test
    void expenseUpdatesBalanceAndTriggersBudgetNoticeWithoutCrossUserAccess() throws Exception {
        String owner = register("finance-owner"), other = register("finance-other");
        String account = mvc.perform(post("/api/accounts").header("Authorization", "Bearer " + owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Main\",\"type\":\"SAVINGS\",\"currency\":\"INR\",\"openingBalance\":10000}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long accountId = json.readTree(account).get("id").asLong();
        String category = mvc.perform(post("/api/categories").header("Authorization", "Bearer " + owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Food\",\"type\":\"EXPENSE\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long categoryId = json.readTree(category).get("id").asLong();
        mvc.perform(post("/api/budgets").header("Authorization", "Bearer " + owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":" + categoryId + ",\"year\":2026,\"month\":11,\"limitAmount\":1000}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/transactions").header("Authorization", "Bearer " + owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":" + accountId + ",\"categoryId\":" + categoryId + ",\"type\":\"EXPENSE\",\"amount\":900,\"transactionDate\":\"2026-11-21\",\"description\":\"Groceries\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.amount").value(900));
        mvc.perform(get("/api/accounts/" + accountId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(9100));
        mvc.perform(get("/api/notifications/unread").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].type").value("BUDGET"));
        mvc.perform(get("/api/accounts/" + accountId).header("Authorization", "Bearer " + other)).andExpect(status().isNotFound());
        mvc.perform(post("/api/transactions").header("Authorization", "Bearer " + owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":" + accountId + ",\"type\":\"EXPENSE\",\"amount\":20000,\"transactionDate\":\"2026-11-21\"}"))
                .andExpect(status().isConflict());
    }
}
