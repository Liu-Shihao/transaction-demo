package com.lsh.transaction.model;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * DTO for creating and updating transactions.
 * This class is used to receive transaction data from API requests.
 */
public class TransactionRequest {
    
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[A-Z0-9]{10,20}$", message = "Account number must be 10-20 alphanumeric characters")
    private String accountNumber;
    
    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "^(DEPOSIT|WITHDRAWAL|TRANSFER)$", message = "Transaction type must be DEPOSIT, WITHDRAWAL, or TRANSFER")
    private String transactionType;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999999.99", message = "Amount cannot exceed 999,999,999.99")
    private BigDecimal amount;
    
    @NotBlank(message = "Description is required")
    @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
    private String description;
    
    // Recipient account validation:
    // - Required for TRANSFER transactions
    // - Optional for DEPOSIT/WITHDRAWAL transactions
    // - When provided, must match pattern: 10-20 uppercase alphanumeric characters
    private String recipientAccount;
    
    // Default constructor
    public TransactionRequest() {}
    
    // Constructor with all fields
    public TransactionRequest(String accountNumber, String transactionType, BigDecimal amount, String description, String recipientAccount) {
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.recipientAccount = recipientAccount;
    }
    
    // Getters and Setters
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getRecipientAccount() {
        return recipientAccount;
    }
    
    public void setRecipientAccount(String recipientAccount) {
        this.recipientAccount = recipientAccount;
    }
    
    // Custom validation method for recipient account
    public boolean isRecipientAccountValid() {
        // If transaction type is TRANSFER, recipient account is required
        if ("TRANSFER".equals(transactionType)) {
            return recipientAccount != null && 
                   !recipientAccount.trim().isEmpty() && 
                   recipientAccount.matches("^[A-Z0-9]{10,20}$");
        }
        // For other transaction types, recipient account is optional
        // If provided, it must match the pattern
        return recipientAccount == null || 
               recipientAccount.trim().isEmpty() || 
               recipientAccount.matches("^[A-Z0-9]{10,20}$");
    }
    
    @Override
    public String toString() {
        return "TransactionRequest{" +
                "accountNumber='" + accountNumber + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", recipientAccount='" + recipientAccount + '\'' +
                '}';
    }
} 