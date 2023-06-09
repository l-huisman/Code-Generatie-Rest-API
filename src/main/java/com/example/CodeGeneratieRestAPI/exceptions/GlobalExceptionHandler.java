package com.example.CodeGeneratieRestAPI.exceptions;

import com.example.CodeGeneratieRestAPI.models.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // Manually handle the exceptions that are thrown by the API
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleAccountNotFoundException(AccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(false, e.getMessage()));
    }

    @ExceptionHandler(AccountNotAccessibleException.class)
    public ResponseEntity<ApiResponse<String>> handleAccountNotAccessibleException(AccountNotAccessibleException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
    }

    @ExceptionHandler(AccountCreationException.class)
    public ResponseEntity<ApiResponse<String>> handleAccountCreationException(AccountCreationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, "Error whilst creating the account: " + e.getMessage()));
    }

    @ExceptionHandler(IBANGenerationException.class)
    public ResponseEntity<ApiResponse<String>> handleIBANGenerationException(IBANGenerationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, "Error whilst generating the IBAN: " + e.getMessage()));
    }

    @ExceptionHandler(AccountNotOwnedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccountNotOwnedException(AccountNotOwnedException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleTransactionNotFoundException(TransactionNotFoundException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
    }

    @ExceptionHandler(TransactionNotOwnedException.class)
    public ResponseEntity<ApiResponse<String>> handleTransactionNotOwnedException(TransactionNotOwnedException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
    }

    @ExceptionHandler(TransactionTransferSavingsException.class)
    public ResponseEntity<ApiResponse<String>> handleTransactionTransferSavingsException(
            TransactionTransferSavingsException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
    }

    @ExceptionHandler(TransactionExceededDailyLimitException.class)
    public ResponseEntity<ApiResponse<String>> handleTransactionExceededDailyLimitException(
            TransactionExceededDailyLimitException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
    }

    @ExceptionHandler(TransactionExceededAbsoluteLimitException.class)
    public ResponseEntity<ApiResponse<String>> handleTransactionExceededAbsoluteLimitException(
            TransactionExceededAbsoluteLimitException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
    }

    @ExceptionHandler(TransactionExceededTransactionLimitException.class)
    public ResponseEntity<ApiResponse<String>> handleTransactionExceededTransactionLimitException(
            TransactionExceededTransactionLimitException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
    }

    @ExceptionHandler(EmployeeOnlyException.class)
    public ResponseEntity<ApiResponse<String>> handleEmployeeOnlyException(EmployeeOnlyException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
    }

    @ExceptionHandler(TransactionAmountNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleTransactionAmountNotValidException(
            TransactionAmountNotValidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
    }

    @ExceptionHandler(TransactionAccountNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleTransactionAccountNotValidException(
            TransactionAccountNotValidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
    }

    @ExceptionHandler(TransactionTypeNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleTransactionTypeNotValidException(
            TransactionTypeNotValidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleUserNotFoundException(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(false, e.getMessage()));
    }

    @ExceptionHandler(UserOnlyException.class)
    public ResponseEntity<ApiResponse<String>> handleUserOnlyException(UserOnlyException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(false, e.getMessage()));
    }

    @ExceptionHandler(UserCreationException.class)
    public ResponseEntity<ApiResponse<String>> handleUserCreationException(UserCreationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, "Error whilst creating the user: " + e.getMessage()));
    }

    // Handle all exceptions that are not handled by other handlers
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "An error occurred: " + e.getMessage()));
    }

}