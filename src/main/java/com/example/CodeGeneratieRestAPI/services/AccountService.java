package com.example.CodeGeneratieRestAPI.services;

import com.example.CodeGeneratieRestAPI.dtos.AccountRequestDTO;
import com.example.CodeGeneratieRestAPI.exceptions.AccountCreationException;
import com.example.CodeGeneratieRestAPI.exceptions.AccountNoDataChangedException;
import com.example.CodeGeneratieRestAPI.exceptions.AccountNotAccessibleException;
import com.example.CodeGeneratieRestAPI.exceptions.AccountNotFoundException;
import com.example.CodeGeneratieRestAPI.exceptions.AccountUpdateException;
import com.example.CodeGeneratieRestAPI.exceptions.UserNotFoundException;
import com.example.CodeGeneratieRestAPI.helpers.ServiceHelper;
import com.example.CodeGeneratieRestAPI.models.Account;
import com.example.CodeGeneratieRestAPI.models.User;
import com.example.CodeGeneratieRestAPI.repositories.AccountRepository;
import com.example.CodeGeneratieRestAPI.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static com.example.CodeGeneratieRestAPI.helpers.IBANGenerator.getUniqueIban;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;

    public Account add(AccountRequestDTO accountRequestDTO) {
        try {
            User currentLoggedInUser = ServiceHelper.getLoggedInUser();

            //  Check if the accountRequestDTO is valid
            //this.checkIfAccountRequestDTOIsValid(accountRequestDTO, currentLoggedInUser);

            //  Check if the IBAN has not been set yet
            if (accountRequestDTO.getIban() != null) {
                throw new AccountCreationException("You cannot set the IBAN of a new account");
            }

            //  If the user is an employee, check if the user id is set
            //  It is possible for an employee to add an account for itself, but it is also possible for an employee to add an account for another user
            //  Hence why we check if the user id is set if the user is an employee
            if (currentLoggedInUser.getUserType().equals("EMPLOYEE") && accountRequestDTO.getUserId() == null) {
                throw new AccountCreationException("You cannot add an account as an employee without selecting a user (if you are adding an account for yourself, select yourself as the user)");
            } else {
                //  If the user is not an employee, set the user id to the id of the current logged in user
                accountRequestDTO.setUserId(currentLoggedInUser.getId());
            }

            //  Get the user
            User user = userRepository.findById(accountRequestDTO.getUserId()).orElse(null);

            //  Set the userId on the account
            accountRequestDTO.setUserId(currentLoggedInUser.getId());

            //  Generate a new unique IBAN
            String iban = getUniqueIban();

            //  Set the IBAN of the accountRequestDTO
            accountRequestDTO.setIban(iban);

            //  Create new account object and save it to the database
            Account newAccount = new Account(accountRequestDTO, user);
            newAccount.setCreatedAt(getCurrentDate());

            accountRepository.save(newAccount);

            return newAccount;
        } catch (Exception e) {
            throw e;
        }

    }

    private Date getCurrentDate() {
        //TODO: Make the ZoneId configurable
        ZoneId zone = ZoneId.of("Europe/Amsterdam");
        return Date.from(LocalDateTime.now(zone).atZone(zone).toInstant());
    }

    private void checkIfAccountRequestDTOIsValid(AccountRequestDTO accountRequestDTO) {
        //  Check if the accountRequestDTO is null
        if (accountRequestDTO == null) {
            throw new IllegalArgumentException("The provided data cannot be null");
        }

        //  Check if any fields other than iban are set, if not, throw an exception because there is nothing to update
        //  This code can throw an IllegalAccessException, which is why this piece of code is in a try catch block
        try {
            boolean allFieldsNull = true;
            for (Field field : accountRequestDTO.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (!field.getName().equals("iban")) {
                    if (field.get(accountRequestDTO) != null) {
                        allFieldsNull = false;
                        break;
                    }
                }
            }
            if (allFieldsNull) {
                throw new AccountNoDataChangedException("At least one field (other then the IBAN) must be filled out");
            }
        } catch (IllegalAccessException e) {
            throw new AccountUpdateException(e.getMessage());
        }


    }

    private Boolean checkIfAccountBelongsToUser(String iban, User loggedInUser) {
        if (loggedInUser.getUserType().equals("EMPLOYEE")) {
            return true;
        }
        return accountRepository.checkIfAccountBelongsToUser(iban, loggedInUser.getId());
    }
    private Account checkAndGetAccount(String iban){
        // Check if the iban is valid
        if (!ServiceHelper.checkIfObjectExistsByIdentifier(iban, new Account())) {
            throw new AccountNotFoundException("Account with IBAN: " + iban + " does not exist");
        }

        // Check if the account belongs to the user or if the user is an employee
        User currentLoggedInUser = ServiceHelper.getLoggedInUser();
        if (!accountRepository.checkIfAccountBelongsToUser(iban, currentLoggedInUser.getId()) && !currentLoggedInUser.getUserType().getAuthority().equals("EMPLOYEE")) {
            throw new AccountNotAccessibleException("Account with IBAN " + iban + " does not belong to you!");
        }

        return accountRepository.getAccountByIban(iban).orElseThrow(() -> new AccountNotFoundException("Account with IBAN: " + iban + " does not exist"));
    }

    public List<Account> getAllActiveAccountsForLoggedInUser(String accountName) {
        // Get the current logged in user
        User currentLoggedInUser = ServiceHelper.getLoggedInUser();

        // Get the balance of all accounts of the user and return it
        List<Account> allActiveAccountsBalance = accountRepository.findAllByNameContainingAndUser_Id(accountName, currentLoggedInUser.getId());

        //  Return the accounts
        return allActiveAccountsBalance;
    }

    public List<Account> getAllAccounts(String search, boolean active) {
        // Get the current logged in user
        User currentLoggedInUser = ServiceHelper.getLoggedInUser();

        // Check if the user is an employee
        if (currentLoggedInUser.getUserType().getAuthority().equals("EMPLOYEE")) {
            //  Get all accounts
            return accountRepository.findAllBySearchTerm();
        }
        else {
            //  Get all accounts of the user
            return accountRepository.findAllBySearchTerm();
        }
    }
    public Account getAccountByIban(String iban) {
        // Get the current logged in user
        User currentLoggedInUser = ServiceHelper.getLoggedInUser();

        //  Check account and get the account
        Account account = this.checkAndGetAccount(iban);

        //  Return the account
        return account;
    }

    public Account update(AccountRequestDTO account) {
        // Get the current logged in user
        User loggedInUser = ServiceHelper.getLoggedInUser();

        // Check if the accountRequestDTO is valid
        this.checkIfAccountRequestDTOIsValid(account);

        // Check if the account exists and get the account
        Account accountToUpdate = checkAndGetAccount(account.getIban());

        // Update the account
        Account updatedAccount = getUpdatedAccount(account, accountToUpdate);

        // Save the account
        accountRepository.save(updatedAccount);

        // Create a response object and return it
        return updatedAccount;
    }

    private Account getUpdatedAccount(AccountRequestDTO accountWithNewValues, Account accountToUpdate) {
        // Loop through all the fields of the accountWithNewValues object
        // If the field is not null and the value is different from the one in the accountToUpdate object
        // Set the new value to the accountToUpdate object
        try {
            for (Field field : accountWithNewValues.getClass().getDeclaredFields()) {
                // Skip the iban and userId fields (they cannot be updated)
                if (field.getName().equals("iban") || field.getName().equals("userId"))
                    continue;

                // Check if the field exists in the Account class
                Field accountField = accountToUpdate.getClass().getDeclaredField(field.getName());
                // Set the field to accessible
                field.setAccessible(true);
                accountField.setAccessible(true);
                Object newValue = field.get(accountWithNewValues);
                Object oldValue = accountField.get(accountToUpdate);

                if (newValue != null && !newValue.equals(oldValue)) {
                    accountField.set(accountToUpdate, newValue);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AccountUpdateException(e.getMessage());
        }
        // Return the updated account
        return accountToUpdate;
    }

    public String delete(String iban) {
        // Get the current logged-in user
        User loggedInUser = ServiceHelper.getLoggedInUser();

        // Check account and get the account
        Account account = this.checkAndGetAccount(iban);

        // Check if the account is active
        if (!account.getIsActive()) {
            throw new IllegalArgumentException("Account with IBAN: " + iban + " is already inactive");
        }

        // Delete the account
        account.setIsActive(false);
        accountRepository.save(account);

        return "Account with IBAN: " + iban + " has been set to inactive";
    }

    public void addSeededAccount(Account account) {
        account.setIban(getUniqueIban());
        accountRepository.save(account);
    }

//    public List<AccountResponseDTO> getAllAccounts() {
//        List<Account> accounts = accountRepository.findAll();
//        List<AccountResponseDTO> accountsresponse = new ArrayList<>();
//        for (Account account : accounts) {
//            accountsresponse.add(new AccountResponseDTO(account));
//        }
//        return accountsresponse;
//    }
}
