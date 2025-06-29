package com.lsh.transaction.controller;

import com.lsh.transaction.model.PaginatedResponse;
import com.lsh.transaction.model.Transaction;
import com.lsh.transaction.model.TransactionRequest;
import com.lsh.transaction.service.TransactionService;
import com.lsh.transaction.service.TransactionStatistics;
import com.lsh.transaction.service.CacheService;
import com.lsh.transaction.annotation.RateLimit;
import com.lsh.transaction.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;

/**
 * REST controller for transaction management.
 * This controller provides all the necessary endpoints for transaction operations.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@CrossOrigin(origins = "*")
@Tag(name = "Transaction Management", description = "Transaction management related APIs")
@Slf4j
public class TransactionController {
    
    private final TransactionService transactionService;
    private final CacheService cacheService;
    
    public TransactionController(TransactionService transactionService, CacheService cacheService) {
        this.transactionService = transactionService;
        this.cacheService = cacheService;
        log.info("TransactionController initialized with services");
    }
    
    /**
     * Create a new transaction.
     * POST /api/v1/transactions
     */
    @PostMapping
    @Operation(
        summary = "Create Transaction",
        description = "Create a new banking transaction record"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Transaction created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Transaction.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"timestamp\":\"2024-01-15T10:30:00\",\"status\":400,\"error\":\"Validation Error\",\"message\":\"Invalid input parameters\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"timestamp\":\"2024-01-15T10:30:00\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}"
                )
            )
        )
    })
    @RateLimit(limit = 3000, window = 60, key = "api:createTransaction", message = "API rate limit exceeded. Please try again later.")
    @RateLimit(limit = 100, window = 60, keyExpression = "#request.accountNumber", message = "Account write operation rate limit exceeded. Please try again later.")
    @CircuitBreaker(name = "transactionCreate", failureRateThreshold = 30.0, minimumNumberOfCalls = 5, message = "Transaction service is temporarily unavailable. Please try again later.")
    public ResponseEntity<Transaction> createTransaction(
            @Parameter(description = "Transaction creation request", required = true)
            @Valid @RequestBody TransactionRequest request) {
        
        long startTime = System.currentTimeMillis();
        log.info("Creating transaction for account: {}, amount: {}, type: {}", 
                request.getAccountNumber(), request.getAmount(), request.getTransactionType());
        
        try {
            Transaction transaction = transactionService.createTransaction(request);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Transaction created successfully - ID: {}, Account: {}, Duration: {}ms", 
                    transaction.getId(), transaction.getAccountNumber(), duration);
            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to create transaction for account: {}, Duration: {}ms, Error: {}", 
                    request.getAccountNumber(), duration, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get a transaction by ID.
     * GET /api/v1/transactions/{id}
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get Transaction by ID",
        description = "Retrieve a specific transaction by its unique identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transaction retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Transaction.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Transaction not found"
        )
    })
    @RateLimit(limit = 10000, window = 60, key = "api:getTransactionById", message = "API rate limit exceeded. Please try again later.")
    public ResponseEntity<Transaction> getTransactionById(
            @Parameter(description = "Transaction ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        
        long startTime = System.currentTimeMillis();
        log.info("Retrieving transaction with ID: {}", id);
        
        try {
            Transaction transaction = transactionService.getTransactionById(id);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Transaction retrieved successfully - ID: {}, Account: {}, Duration: {}ms", 
                    id, transaction.getAccountNumber(), duration);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to retrieve transaction with ID: {}, Duration: {}ms, Error: {}", 
                    id, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get all transactions with pagination.
     * GET /api/v1/transactions
     */
    @GetMapping
    @Operation(
        summary = "Get All Transactions",
        description = "Retrieve all transactions with pagination support"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transactions retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaginatedResponse.class)
            )
        )
    })
    @RateLimit(limit = 10000, window = 60, key = "api:getAllTransactions", message = "API rate limit exceeded. Please try again later.")
    public ResponseEntity<PaginatedResponse<Transaction>> getAllTransactions(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        
        long startTime = System.currentTimeMillis();
        log.info("Retrieving all transactions - Page: {}, Size: {}", page, size);
        
        try {
            PaginatedResponse<Transaction> response = transactionService.getAllTransactions(page, size);
            long duration = System.currentTimeMillis() - startTime;
            log.info("All transactions retrieved successfully - Page: {}, Size: {}, Total: {}, Duration: {}ms", 
                    page, size, response.getTotalElements(), duration);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to retrieve all transactions - Page: {}, Size: {}, Duration: {}ms, Error: {}", 
                    page, size, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get transactions by account number.
     * GET /api/v1/transactions/account/{accountNumber}
     */
    @GetMapping("/account/{accountNumber}")
    @Operation(
        summary = "Get Transactions by Account",
        description = "Retrieve all transactions for a specific account number"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transactions retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaginatedResponse.class)
            )
        )
    })
    @RateLimit(limit = 10000, window = 60, key = "api:getTransactionsByAccount", message = "API rate limit exceeded. Please try again later.")
    @RateLimit(limit = 500, window = 60, keyExpression = "#accountNumber", message = "Account read operation rate limit exceeded. Please try again later.")
    public ResponseEntity<PaginatedResponse<Transaction>> getTransactionsByAccount(
            @Parameter(description = "Account number", required = true, example = "ACC001")
            @PathVariable String accountNumber,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        
        long startTime = System.currentTimeMillis();
        log.info("Retrieving transactions for account: {} - Page: {}, Size: {}", accountNumber, page, size);
        
        try {
            PaginatedResponse<Transaction> response = transactionService.getTransactionsByAccount(accountNumber, page, size);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Account transactions retrieved successfully - Account: {}, Page: {}, Size: {}, Total: {}, Duration: {}ms", 
                    accountNumber, page, size, response.getTotalElements(), duration);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to retrieve transactions for account: {} - Page: {}, Size: {}, Duration: {}ms, Error: {}", 
                    accountNumber, page, size, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get transactions by transaction type.
     * GET /api/v1/transactions/type/{transactionType}
     */
    @GetMapping("/type/{transactionType}")
    @Operation(
        summary = "Get Transactions by Type",
        description = "Retrieve all transactions of a specific type"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transactions retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaginatedResponse.class)
            )
        )
    })
    @RateLimit(limit = 10000, window = 60, key = "api:getTransactionsByType", message = "API rate limit exceeded. Please try again later.")
    public ResponseEntity<PaginatedResponse<Transaction>> getTransactionsByType(
            @Parameter(description = "Transaction type", required = true, example = "DEPOSIT")
            @PathVariable String transactionType,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        
        long startTime = System.currentTimeMillis();
        log.info("Retrieving transactions by type: {} - Page: {}, Size: {}", transactionType, page, size);
        
        try {
            PaginatedResponse<Transaction> response = transactionService.getTransactionsByType(transactionType, page, size);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Type transactions retrieved successfully - Type: {}, Page: {}, Size: {}, Total: {}, Duration: {}ms", 
                    transactionType, page, size, response.getTotalElements(), duration);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to retrieve transactions by type: {} - Page: {}, Size: {}, Duration: {}ms, Error: {}", 
                    transactionType, page, size, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Update an existing transaction.
     * PUT /api/v1/transactions/{id}
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Update Transaction",
        description = "Update transaction record information by ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transaction updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Transaction.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Transaction not found"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters"
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"timestamp\":\"2024-01-15T10:30:00\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}"
                )
            )
        )
    })
    @RateLimit(limit = 3000, window = 60, key = "api:updateTransaction", message = "API rate limit exceeded. Please try again later.")
    @RateLimit(limit = 100, window = 60, keyExpression = "#request.accountNumber", message = "Account write operation rate limit exceeded. Please try again later.")
    public ResponseEntity<Transaction> updateTransaction(
            @Parameter(description = "Transaction ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Parameter(description = "Transaction update request", required = true)
            @Valid @RequestBody TransactionRequest request) {
        
        long startTime = System.currentTimeMillis();
        log.info("Updating transaction with ID: {} for account: {}, amount: {}", 
                id, request.getAccountNumber(), request.getAmount());
        
        try {
            Transaction transaction = transactionService.updateTransaction(id, request);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Transaction updated successfully - ID: {}, Account: {}, Duration: {}ms", 
                    id, transaction.getAccountNumber(), duration);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to update transaction with ID: {} - Duration: {}ms, Error: {}", 
                    id, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Delete a transaction by ID.
     * DELETE /api/v1/transactions/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete Transaction",
        description = "Delete a transaction record by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transaction deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Transaction not found"
        )
    })
    @RateLimit(limit = 1500, window = 60, key = "api:deleteTransaction", message = "API rate limit exceeded. Please try again later.")
    public ResponseEntity<Map<String, String>> deleteTransaction(
            @Parameter(description = "Transaction ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        
        long startTime = System.currentTimeMillis();
        log.info("Deleting transaction with ID: {}", id);
        
        try {
            boolean deleted = transactionService.deleteTransaction(id);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Transaction deletion completed - ID: {}, Success: {}, Duration: {}ms", 
                    id, deleted, duration);
            
            Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "Transaction deleted successfully");
            response.put("id", id.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to delete transaction with ID: {} - Duration: {}ms, Error: {}", 
                    id, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get transaction statistics.
     * GET /api/v1/transactions/statistics
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "Get Transaction Statistics",
        description = "Get transaction system statistics including total count, amounts, etc."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TransactionStatistics.class)
            )
        )
    })
    @RateLimit(limit = 10000, window = 60, key = "api:getTransactionStatistics", message = "API rate limit exceeded. Please try again later.")
    public ResponseEntity<TransactionStatistics> getTransactionStatistics() {
        long startTime = System.currentTimeMillis();
        log.info("Retrieving transaction statistics");
        
        try {
            TransactionStatistics statistics = transactionService.getTransactionStatistics();
            long duration = System.currentTimeMillis() - startTime;
            log.info("Transaction statistics retrieved successfully - Total: {}, TotalAmount: {}, Duration: {}ms", 
                    statistics.getTotalTransactions(), statistics.getTotalAmount(), duration);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to retrieve transaction statistics - Duration: {}ms, Error: {}", 
                    duration, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Health check endpoint.
     * GET /api/v1/transactions/health
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health Check",
        description = "Check the health status of the transaction service"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Service is running normally",
            content = @Content(
                mediaType = "text/plain",
                examples = @ExampleObject(value = "Transaction Service is running")
            )
        )
    })
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Transaction Service is running");
    }
    
    /**
     * Cache monitoring endpoint.
     * GET /api/v1/transactions/cache/stats
     */
    @GetMapping("/cache/stats")
    @Operation(
        summary = "Cache Statistics",
        description = "Get cache statistics for monitoring and debugging"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cache statistics retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"transactions\":\"Cache 'transactions' size: 10\",\"transactionStats\":\"Cache 'transactionStats' size: 1\"}"
                )
            )
        )
    })
    public ResponseEntity<Map<String, String>> getCacheStats() {
        long startTime = System.currentTimeMillis();
        log.info("Retrieving cache statistics");
        
        try {
            Map<String, String> stats = new java.util.HashMap<>();
            stats.put("transactions", cacheService.getCacheStats("transactions"));
            stats.put("transactionStats", cacheService.getCacheStats("transactionStats"));
            long duration = System.currentTimeMillis() - startTime;
            log.info("Cache statistics retrieved successfully - Duration: {}ms", duration);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to retrieve cache statistics - Duration: {}ms, Error: {}", 
                    duration, e.getMessage(), e);
            throw e;
        }
    }
} 