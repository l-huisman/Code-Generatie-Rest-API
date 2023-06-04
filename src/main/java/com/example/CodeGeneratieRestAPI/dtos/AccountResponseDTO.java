package com.example.CodeGeneratieRestAPI.dtos;

import com.example.CodeGeneratieRestAPI.models.Account;
import lombok.Data;

import java.util.Date;

@Data
public class AccountResponseDTO {
    private Long userId;
    private String iban;
    private String accountName;
    private Float dailyLimit;
    private Float transactionLimit;
    private Float absoluteLimit;
    private Float balance;
    private Boolean isSavings;
    private Boolean isActive;
    private Date createdAt;

    // A constructor that takes an Account object
    public AccountResponseDTO(Account account) {
        this.userId = account.getUserId();
        this.iban = account.getIban();
        this.accountName = account.getName();
        this.dailyLimit = account.getDailyLimit();
        this.transactionLimit = account.getTransactionLimit();
        this.absoluteLimit = account.getAbsoluteLimit();
        this.balance = account.getBalance();
        this.isSavings = account.getIsSavings();
        this.isActive = account.getIsActive();
        this.createdAt = account.getCreatedAt();
    }
}
