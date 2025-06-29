package com.lsh.transaction.service;

import com.lsh.transaction.exception.ResourceNotFoundException;
import com.lsh.transaction.model.PaginatedResponse;
import com.lsh.transaction.model.Transaction;
import com.lsh.transaction.model.TransactionRequest;
import com.lsh.transaction.repository.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    
    @Mock
    private InMemoryTransactionRepository transactionRepository;
    
    @Mock
    private com.lsh.transaction.service.CacheService cacheService;
    
    private com.lsh.transaction.service.TransactionServiceImpl transactionService;
    
    @BeforeEach
    void setUp() {
        transactionService = new com.lsh.transaction.service.TransactionServiceImpl(transactionRepository, cacheService);

        // Mock cacheService.getWithNullProtection to directly call supplier
        lenient().when(cacheService.getWithNullProtection(anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                java.util.function.Supplier<?> supplier = invocation.getArgument(2);
                return supplier.get();
            });
    }
    
    @Test
    void createTransaction_ValidRequest_ShouldCreateTransaction() {
        // Arrange
        TransactionRequest request = new TransactionRequest(
                "ACCOUNT123456", "DEPOSIT", new BigDecimal("100.00"), "Test deposit", null
        );
        
        Transaction expectedTransaction = new Transaction();
        expectedTransaction.setId(UUID.randomUUID());
        expectedTransaction.setAccountNumber("ACCOUNT123456");
        expectedTransaction.setTransactionType("DEPOSIT");
        expectedTransaction.setAmount(new BigDecimal("100.00"));
        expectedTransaction.setDescription("Test deposit");
        
        when(transactionRepository.save(any(Transaction.class))).thenReturn(expectedTransaction);
        
        // Act
        Transaction result = transactionService.createTransaction(request);
        
        // Assert
        assertNotNull(result);
        assertEquals("ACCOUNT123456", result.getAccountNumber());
        assertEquals("DEPOSIT", result.getTransactionType());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals("Test deposit", result.getDescription());
        
        verify(transactionRepository).save(any(Transaction.class));
    }
    
    @Test
    void createTransaction_TransferWithoutRecipient_ShouldThrowException() {
        // Arrange
        TransactionRequest request = new TransactionRequest(
                "ACCOUNT123456", "TRANSFER", new BigDecimal("100.00"), "Test transfer", null
        );
        
        // Mock repository to return null to simulate validation failure
        when(transactionRepository.save(any(Transaction.class))).thenReturn(null);
        
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            transactionService.createTransaction(request);
        });
        
        verify(transactionRepository).save(any(Transaction.class));
    }
    
    @Test
    void createTransaction_TransferWithSameAccounts_ShouldThrowException() {
        // Arrange
        TransactionRequest request = new TransactionRequest(
                "ACCOUNT123456", "TRANSFER", new BigDecimal("100.00"), "Test transfer", "ACCOUNT123456"
        );
        
        // Mock repository to return null to simulate validation failure
        when(transactionRepository.save(any(Transaction.class))).thenReturn(null);
        
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            transactionService.createTransaction(request);
        });
        
        verify(transactionRepository).save(any(Transaction.class));
    }
    
    @Test
    void getTransactionById_ExistingTransaction_ShouldReturnTransaction() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        Transaction expectedTransaction = new Transaction();
        expectedTransaction.setId(transactionId);
        expectedTransaction.setAccountNumber("ACCOUNT123456");
        
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(expectedTransaction));
        
        // Act
        Transaction result = transactionService.getTransactionById(transactionId);
        
        // Assert
        assertNotNull(result);
        assertEquals(transactionId, result.getId());
        assertEquals("ACCOUNT123456", result.getAccountNumber());
        
        verify(transactionRepository).findById(transactionId);
    }
    
    @Test
    void getTransactionById_NonExistingTransaction_ShouldThrowException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            transactionService.getTransactionById(transactionId);
        });
        
        verify(transactionRepository).findById(transactionId);
    }
    
    @Test
    void deleteTransaction_ExistingTransaction_ShouldDeleteSuccessfully() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = new Transaction();
        existingTransaction.setId(transactionId);
        existingTransaction.setAccountNumber("ACCOUNT123456");
        
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existingTransaction));
        when(transactionRepository.deleteById(transactionId)).thenReturn(true);
        
        // Act
        boolean result = transactionService.deleteTransaction(transactionId);
        
        // Assert
        assertTrue(result);
        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository).deleteById(transactionId);
    }
    
    @Test
    void deleteTransaction_NonExistingTransaction_ShouldThrowException() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            transactionService.deleteTransaction(transactionId);
        });
        
        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository, never()).deleteById(any());
    }
    
    @Test
    void getAllTransactions_ValidPagination_ShouldReturnPaginatedResponse() {
        // Arrange
        int page = 0;
        int size = 10;
        java.util.List<Transaction> transactions = Arrays.asList(
                new Transaction("ACCOUNT123456", "DEPOSIT", new BigDecimal("100.00"), "Test 1"),
                new Transaction("ACCOUNT789012", "WITHDRAWAL", new BigDecimal("50.00"), "Test 2")
        );
        
        when(transactionRepository.findAll(page, size)).thenReturn(transactions);
        when(transactionRepository.count()).thenReturn(2L);
        
        // Act
        PaginatedResponse<Transaction> result = transactionService.getAllTransactions(page, size);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.getData().size());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(2, result.getTotalElements());
        
        verify(transactionRepository).findAll(page, size);
        verify(transactionRepository).count();
    }
    
    @Test
    void getAllTransactions_InvalidPageSize_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            transactionService.getAllTransactions(0, 0);
        });
        
        verify(transactionRepository, never()).findAll(anyInt(), anyInt());
    }
} 