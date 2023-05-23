package com.example.CodeGeneratieRestAPI.dtos;

import com.example.CodeGeneratieRestAPI.models.TransactionType;
import lombok.Data;

@Data
public class TransactionRequestDTO {
    private long fromAccountIban;
    private long toAccountIban;
    private TransactionType transactionType;
    private Float amount;
    private String label;
    private String description;
}
