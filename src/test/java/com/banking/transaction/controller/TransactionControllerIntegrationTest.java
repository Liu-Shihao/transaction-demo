package com.banking.transaction.controller;

import com.banking.transaction.model.TransactionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("local")
class TransactionControllerIntegrationTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
    
    @Test
    void contextLoads() {
        assertNotNull(webApplicationContext);
    }
    
    @Test
    void createTransaction_ValidRequest_ShouldReturnCreatedTransaction() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest(
                "ACCOUNT123456", "DEPOSIT", new BigDecimal("100.00"), "Test deposit", null
        );
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("ACCOUNT123456"))
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value("100.0"))
                .andExpect(jsonPath("$.description").value("Test deposit"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
    
    @Test
    void createTransaction_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest(
                "", "INVALID_TYPE", new BigDecimal("-100.00"), "", null
        );
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void getAllTransactions_ShouldReturnPaginatedResponse() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists());
    }
    
    @Test
    void getTransactionById_ExistingTransaction_ShouldReturnTransaction() throws Exception {
        // First create a transaction
        TransactionRequest request = new TransactionRequest(
                "ACCOUNT123456", "DEPOSIT", new BigDecimal("100.00"), "Test deposit", null
        );
        
        String response = mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Extract the ID from the response
        String id = objectMapper.readTree(response).get("id").asText();
        
        // Now get the transaction by ID
        mockMvc.perform(get("/api/v1/transactions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.accountNumber").value("ACCOUNT123456"));
    }
    
    @Test
    void getTransactionById_NonExistingTransaction_ShouldReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void healthCheck_ShouldReturnOk() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Transaction Service is running"));
    }
    
    @Test
    void getTransactionStatistics_ShouldReturnStatistics() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").exists())
                .andExpect(jsonPath("$.totalAmount").exists())
                .andExpect(jsonPath("$.transactionsByType").exists())
                .andExpect(jsonPath("$.amountByType").exists());
    }
} 