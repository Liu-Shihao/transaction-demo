package com.lsh.transaction.controller;

import com.lsh.transaction.model.Transaction;
import com.lsh.transaction.model.TransactionRequest;
import com.lsh.transaction.model.PaginatedResponse;
import com.lsh.transaction.service.VirtualThreadTransactionService;
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
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Virtual Thread Enhanced REST Controller for transaction management.
 * This controller demonstrates the use of virtual threads for better concurrency performance.
 */
@RestController
@RequestMapping("/api/v2/transactions")
@CrossOrigin(origins = "*")
@Tag(name = "Virtual Thread Transactions", description = "Transaction operations using virtual threads")
@Slf4j
public class VirtualThreadController {

    private final VirtualThreadTransactionService virtualThreadService;

    public VirtualThreadController(VirtualThreadTransactionService virtualThreadService) {
        this.virtualThreadService = virtualThreadService;
        log.info("VirtualThreadController initialized with virtual thread service");
    }

    /**
     * Create a new transaction using virtual threads.
     * POST /api/v2/transactions
     */
    @PostMapping
    @Operation(
        summary = "Create Transaction (Virtual Thread)",
        description = "Create a new banking transaction record using virtual threads for better performance"
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
            description = "Invalid request parameters"
        )
    })
    public CompletableFuture<ResponseEntity<Transaction>> createTransaction(
            @Parameter(description = "Transaction creation request", required = true)
            @Valid @RequestBody TransactionRequest request) {
        
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        log.info("Creating transaction with virtual thread - Thread: {}, Account: {}, Amount: {}, Type: {}", 
                threadName, request.getAccountNumber(), request.getAmount(), request.getTransactionType());
        
        return virtualThreadService.createTransactionAsync(request)
                .thenApply(transaction -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Virtual thread transaction created successfully - Thread: {}, ID: {}, Account: {}, Duration: {}ms", 
                            threadName, transaction.getId(), transaction.getAccountNumber(), duration);
                    return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
                })
                .exceptionally(throwable -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Virtual thread transaction creation failed - Thread: {}, Account: {}, Duration: {}ms, Error: {}", 
                            threadName, request.getAccountNumber(), duration, throwable.getMessage(), throwable);
                    
                    // Unwrap the original exception if it's wrapped in RuntimeException
                    Throwable originalException = throwable;
                    if (throwable instanceof RuntimeException && throwable.getCause() != null) {
                        originalException = throwable.getCause();
                    }
                    
                    // Re-throw the original exception to preserve the error details
                    if (originalException instanceof RuntimeException) {
                        throw (RuntimeException) originalException;
                    } else {
                        throw new RuntimeException(originalException);
                    }
                });
    }

    /**
     * Get a transaction by ID using virtual threads.
     * GET /api/v2/transactions/{id}
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get Transaction by ID (Virtual Thread)",
        description = "Retrieve a specific transaction by its unique identifier using virtual threads"
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
    public CompletableFuture<ResponseEntity<Transaction>> getTransactionById(
            @Parameter(description = "Transaction ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        log.info("Retrieving transaction with virtual thread - Thread: {}, ID: {}", threadName, id);
        
        return virtualThreadService.getTransactionByIdAsync(id)
                .thenApply(transaction -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Virtual thread transaction retrieved successfully - Thread: {}, ID: {}, Account: {}, Duration: {}ms", 
                            threadName, id, transaction.getAccountNumber(), duration);
                    return ResponseEntity.ok(transaction);
                })
                .exceptionally(throwable -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Virtual thread transaction retrieval failed - Thread: {}, ID: {}, Duration: {}ms, Error: {}", 
                            threadName, id, duration, throwable.getMessage(), throwable);
                    
                    // Unwrap the original exception if it's wrapped in RuntimeException
                    Throwable originalException = throwable;
                    if (throwable instanceof RuntimeException && throwable.getCause() != null) {
                        originalException = throwable.getCause();
                    }
                    
                    // Re-throw the original exception to preserve the error details
                    if (originalException instanceof RuntimeException) {
                        throw (RuntimeException) originalException;
                    } else {
                        throw new RuntimeException(originalException);
                    }
                });
    }

    /**
     * Get all transactions with pagination using virtual threads.
     * GET /api/v2/transactions
     */
    @GetMapping
    @Operation(
        summary = "Get All Transactions (Virtual Thread)",
        description = "Retrieve all transactions with pagination support using virtual threads"
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
    public CompletableFuture<ResponseEntity<PaginatedResponse<Transaction>>> getAllTransactions(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        log.info("Retrieving all transactions with virtual thread - Thread: {}, Page: {}, Size: {}", threadName, page, size);
        
        return virtualThreadService.getAllTransactionsAsync(page, size)
                .thenApply(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Virtual thread all transactions retrieved successfully - Thread: {}, Page: {}, Size: {}, Total: {}, Duration: {}ms", 
                            threadName, page, size, response.getTotalElements(), duration);
                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Virtual thread all transactions retrieval failed - Thread: {}, Page: {}, Size: {}, Duration: {}ms, Error: {}", 
                            threadName, page, size, duration, throwable.getMessage(), throwable);
                    throw new RuntimeException(throwable);
                });
    }

    /**
     * Get transactions by account number with pagination using virtual threads.
     * GET /api/v2/transactions/account/{accountNumber}?page=0&size=10
     */
    @GetMapping("/account/{accountNumber}")
    @Operation(
        summary = "Get Transactions by Account (Virtual Thread)",
        description = "Get transaction records by account number with pagination using virtual threads"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Account transactions retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaginatedResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid account number format"
        )
    })
    public CompletableFuture<ResponseEntity<PaginatedResponse<Transaction>>> getTransactionsByAccount(
            @Parameter(description = "Account number", required = true, example = "ACCOUNT123456")
            @PathVariable String accountNumber,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return virtualThreadService.getTransactionsByAccountAsync(accountNumber, page, size)
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Get transactions by transaction type with pagination using virtual threads.
     * GET /api/v2/transactions/type/{transactionType}?page=0&size=10
     */
    @GetMapping("/type/{transactionType}")
    @Operation(
        summary = "Get Transactions by Type (Virtual Thread)",
        description = "Get transaction records by transaction type with pagination using virtual threads"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Type-based transactions retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaginatedResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid transaction type"
        )
    })
    public CompletableFuture<ResponseEntity<PaginatedResponse<Transaction>>> getTransactionsByType(
            @Parameter(description = "Transaction type", required = true, example = "DEPOSIT", schema = @Schema(allowableValues = {"DEPOSIT", "WITHDRAWAL", "TRANSFER"}))
            @PathVariable String transactionType,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return virtualThreadService.getTransactionsByTypeAsync(transactionType, page, size)
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Update an existing transaction using virtual threads.
     * PUT /api/v2/transactions/{id}
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Update Transaction (Virtual Thread)",
        description = "Update transaction record information by ID using virtual threads"
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
        )
    })
    public CompletableFuture<ResponseEntity<Transaction>> updateTransaction(
            @Parameter(description = "Transaction ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Parameter(description = "Transaction update request", required = true)
            @Valid @RequestBody TransactionRequest request) {
        
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        log.info("Updating transaction with virtual thread - Thread: {}, ID: {}, Account: {}, Amount: {}", 
                threadName, id, request.getAccountNumber(), request.getAmount());
        
        return virtualThreadService.updateTransactionAsync(id, request)
                .thenApply(transaction -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Virtual thread transaction updated successfully - Thread: {}, ID: {}, Account: {}, Duration: {}ms", 
                            threadName, id, transaction.getAccountNumber(), duration);
                    return ResponseEntity.ok(transaction);
                })
                .exceptionally(throwable -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Virtual thread transaction update failed - Thread: {}, ID: {}, Duration: {}ms, Error: {}", 
                            threadName, id, duration, throwable.getMessage(), throwable);
                    
                    // Unwrap the original exception if it's wrapped in RuntimeException
                    Throwable originalException = throwable;
                    if (throwable instanceof RuntimeException && throwable.getCause() != null) {
                        originalException = throwable.getCause();
                    }
                    
                    // Re-throw the original exception to preserve the error details
                    if (originalException instanceof RuntimeException) {
                        throw (RuntimeException) originalException;
                    } else {
                        throw new RuntimeException(originalException);
                    }
                });
    }

    /**
     * Delete a transaction by ID using virtual threads.
     * DELETE /api/v2/transactions/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete Transaction (Virtual Thread)",
        description = "Delete a transaction record by its ID using virtual threads"
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
    public CompletableFuture<ResponseEntity<Map<String, String>>> deleteTransaction(
            @Parameter(description = "Transaction ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        log.info("Deleting transaction with virtual thread - Thread: {}, ID: {}", threadName, id);
        
        return virtualThreadService.deleteTransactionAsync(id)
                .thenApply(deleted -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Virtual thread transaction deletion completed - Thread: {}, ID: {}, Success: {}, Duration: {}ms", 
                            threadName, id, deleted, duration);
                    
                    Map<String, String> response = new java.util.HashMap<>();
                    response.put("message", "Transaction deleted successfully");
                    response.put("id", id.toString());
                    response.put("deleted", String.valueOf(deleted));
                    return ResponseEntity.ok(response);
                })
                .exceptionally(throwable -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Virtual thread transaction deletion failed - Thread: {}, ID: {}, Duration: {}ms, Error: {}", 
                            threadName, id, duration, throwable.getMessage(), throwable);
                    throw new RuntimeException(throwable);
                });
    }

    /**
     * Get transaction statistics using virtual threads.
     * GET /api/v2/transactions/statistics
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "Get Transaction Statistics (Virtual Thread)",
        description = "Get transaction system statistics including total count, amounts, etc. using virtual threads"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VirtualThreadTransactionService.TransactionStatistics.class)
            )
        )
    })
    public CompletableFuture<ResponseEntity<VirtualThreadTransactionService.TransactionStatistics>> getTransactionStatistics() {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        log.info("Retrieving transaction statistics with virtual thread - Thread: {}", threadName);
        
        return virtualThreadService.getTransactionStatisticsAsync()
                .thenApply(statistics -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Virtual thread transaction statistics retrieved successfully - Thread: {}, Total: {}, TotalAmount: {}, Duration: {}ms", 
                            threadName, statistics.getTotalTransactions(), statistics.getTotalAmount(), duration);
                    return ResponseEntity.ok(statistics);
                })
                .exceptionally(throwable -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Virtual thread transaction statistics retrieval failed - Thread: {}, Duration: {}ms, Error: {}", 
                            threadName, duration, throwable.getMessage(), throwable);
                    throw new RuntimeException(throwable);
                });
    }

    /**
     * Health check endpoint for virtual threads.
     * GET /api/v2/transactions/health
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health Check (Virtual Thread)",
        description = "Check the health status of the virtual thread transaction service"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Service is running normally",
            content = @Content(
                mediaType = "text/plain",
                examples = @ExampleObject(value = "Virtual Thread Transaction Service is running")
            )
        )
    })
    public CompletableFuture<ResponseEntity<String>> health() {
        return CompletableFuture.supplyAsync(() -> "Virtual Thread Transaction Service is running")
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Cache monitoring endpoint for virtual threads.
     * GET /api/v2/transactions/cache/stats
     */
    @GetMapping("/cache/stats")
    @Operation(
        summary = "Cache Statistics (Virtual Thread)",
        description = "Get cache statistics for monitoring and debugging virtual thread operations"
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
    public CompletableFuture<ResponseEntity<Map<String, String>>> getCacheStats() {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        log.info("Retrieving cache statistics with virtual thread - Thread: {}", threadName);
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> stats = new java.util.HashMap<>();
            stats.put("transactions", "Cache 'transactions' size: N/A (Virtual Thread)");
            stats.put("transactionStats", "Cache 'transactionStats' size: N/A (Virtual Thread)");
            stats.put("threadName", threadName);
            return stats;
        }).thenApply(stats -> {
            long duration = System.currentTimeMillis() - startTime;
            log.info("Virtual thread cache statistics retrieved successfully - Thread: {}, Duration: {}ms", threadName, duration);
            return ResponseEntity.ok(stats);
        }).exceptionally(throwable -> {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Virtual thread cache statistics retrieval failed - Thread: {}, Duration: {}ms, Error: {}", 
                    threadName, duration, throwable.getMessage(), throwable);
            throw new RuntimeException(throwable);
        });
    }

    // Additional virtual thread specific endpoints

    @PostMapping("/batch")
    @Operation(summary = "Create multiple transactions in batch using virtual threads")
    public CompletableFuture<ResponseEntity<List<Transaction>>> createTransactionsBatchAsync(
            @RequestBody List<TransactionRequest> requests) {
        
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        log.info("Creating batch transactions with virtual thread - Thread: {}, Count: {}", threadName, requests.size());
        
        return virtualThreadService.createTransactionsBatchAsync(requests)
                .thenApply(transactions -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Virtual thread batch transactions created successfully - Thread: {}, Count: {}, Duration: {}ms", 
                            threadName, transactions.size(), duration);
                    return ResponseEntity.ok(transactions);
                })
                .exceptionally(throwable -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Virtual thread batch transactions creation failed - Thread: {}, Count: {}, Duration: {}ms, Error: {}", 
                            threadName, requests.size(), duration, throwable.getMessage(), throwable);
                    throw new RuntimeException(throwable);
                });
    }

    @GetMapping("/search")
    @Operation(summary = "Search transactions asynchronously")
    public CompletableFuture<ResponseEntity<List<Transaction>>> searchTransactionsAsync(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        log.info("Searching transactions with virtual thread - Thread: {}, Account: {}, Type: {}, Page: {}, Size: {}", 
                threadName, accountNumber, type, page, size);
        
        return virtualThreadService.searchTransactionsAsync(accountNumber, type, page, size)
                .thenApply(transactions -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Virtual thread transaction search completed successfully - Thread: {}, Results: {}, Duration: {}ms", 
                            threadName, transactions.size(), duration);
                    return ResponseEntity.ok(transactions);
                })
                .exceptionally(throwable -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Virtual thread transaction search failed - Thread: {}, Duration: {}ms, Error: {}", 
                            threadName, duration, throwable.getMessage(), throwable);
                    throw new RuntimeException(throwable);
                });
    }

    @GetMapping("/health/virtual-threads")
    @Operation(summary = "Virtual thread health check")
    public CompletableFuture<ResponseEntity<String>> virtualThreadHealthCheck() {
        String threadName = Thread.currentThread().getName();
        log.info("Virtual thread specific health check - Thread: {}", threadName);
        return CompletableFuture.supplyAsync(() -> "Virtual threads are working correctly!")
                .thenApply(ResponseEntity::ok);
    }
} 