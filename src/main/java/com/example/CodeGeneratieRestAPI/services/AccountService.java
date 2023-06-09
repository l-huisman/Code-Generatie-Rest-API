package com.example.CodeGeneratieRestAPI.services;

import com.example.CodeGeneratieRestAPI.dtos.AccountData;
import com.example.CodeGeneratieRestAPI.dtos.AccountLimitsLeft;
import com.example.CodeGeneratieRestAPI.dtos.AccountRequestDTO;
import com.example.CodeGeneratieRestAPI.dtos.AccountResponseDTO;
import com.example.CodeGeneratieRestAPI.dtos.UserRequestDTO;
import com.example.CodeGeneratieRestAPI.exceptions.*;
import com.example.CodeGeneratieRestAPI.helpers.IBANGenerator;
import com.example.CodeGeneratieRestAPI.helpers.LoggedInUserHelper;
import com.example.CodeGeneratieRestAPI.helpers.ServiceHelper;
import com.example.CodeGeneratieRestAPI.models.Account;
import com.example.CodeGeneratieRestAPI.models.HashedPassword;
import com.example.CodeGeneratieRestAPI.models.User;
import com.example.CodeGeneratieRestAPI.models.UserType;
import com.example.CodeGeneratieRestAPI.repositories.AccountRepository;
import com.example.CodeGeneratieRestAPI.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;


@Service
public class AccountService {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ServiceHelper serviceHelper;
    @Autowired
    private IBANGenerator ibanGenerator;
    @Autowired
    private TransactionService transactionService;

    @Autowired
    private LoggedInUserHelper loggedInUserHelper;

    public Account add(AccountRequestDTO accountRequestDTO, User loggedInUser) {
        try {
            //  Check if the accountRequestDTO is valid
            this.checkIfAccountRequestDTOIsValidForAdd(accountRequestDTO);

            //  Check if the IBAN has not been set yet
            if (accountRequestDTO.getIban() != null) {
                //  To add the bank's own account, this is a special case
                if (accountRequestDTO.getIban().equals("NL01-INHO-0000-0000-01"))
                    return addBankAccount(accountRequestDTO);
                throw new AccountCreationException("You cannot set the IBAN of a new account");
            }

            //  If the user is an employee, check if the user id is set
            //  It is possible for an employee to add an account for itself, but it is also possible for an employee to add an account for another user
            //  Hence why we check if the user id is set if the user is an employee
            if (loggedInUser.getUserType().equals(UserType.EMPLOYEE) && accountRequestDTO.getUserId() == null) {
                throw new AccountCreationException("You cannot add an account as an employee without selecting a user (if you are adding an account for yourself, select yourself as the user)");
            } else {
                //  If the user is not an employee, set the user id to the id of the current logged-in user
                accountRequestDTO.setUserId(loggedInUser.getId());
            }

            //  Get the user
            User user = userRepository.findById(accountRequestDTO.getUserId()).orElse(null);

            //  Generate a new unique IBAN and set it to the accountRequestDTO
            accountRequestDTO.setIban(ibanGenerator.getUniqueIban());

            //  Create new account object and save it to the database
            Account newAccount = new Account(accountRequestDTO, user);
            newAccount.setCreatedAt(getCurrentDate());

            accountRepository.save(newAccount);

            return newAccount;
        } catch (Exception e) {
            throw e;
        }

    }

    private Account addBankAccount(AccountRequestDTO accountRequestDTO) {
        //  Check if the account with this IBAN already exists
        if (serviceHelper.checkIfObjectExistsByIdentifier(accountRequestDTO.getIban(), new Account())) {
            throw new AccountAlreadyExistsException("The bank's account already exists");
        }
        return accountRepository.save(new Account(accountRequestDTO, null));
    }

    private Date getCurrentDate() {
        ZoneId zone = ZoneId.of("Europe/Amsterdam");
        return Date.from(LocalDateTime.now(zone).atZone(zone).toInstant());
    }

    private void checkIfAccountRequestDTOIsValidForAdd(AccountRequestDTO accountRequestDTO) {
        //  Check if the accountRequestDTO is null
        if (accountRequestDTO == null) {
            throw new IllegalArgumentException("The provided data cannot be null");
        }
        //  The balance must always be 0 when creating a new account
        accountRequestDTO.setBalance(0.0F);

        //  Check if all fields other than iban and userId are set, if not, throw an exception because the required fields are not set
        //  This code can throw an IllegalAccessException, which is why this piece of code is in a try catch block
        try {
            for (Field field : accountRequestDTO.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                //  The userId is allowed to be null if the user is adding an account for itself
                //  If the user is an employee, the userId must be set, but this is checked later
                //  The IBAN is required to be null, because it will be generated later, the exception handling for this is done in the add method
                if (!field.getName().equals("iban") && !field.getName().equals("userId")) {
                    if (field.get(accountRequestDTO) == null) {
                        throw new AccountCreationException("All fields (other then the IBAN) must be filled out");
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new AccountUpdateException(e.getMessage());
        }
    }

    private void checkIfAccountRequestDTOIsValidForUpdate(AccountRequestDTO accountRequestDTO) {
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

    private Boolean checkIfAccountBelongsToUser(Account account, User loggedInUser) {
        if (loggedInUser.getUserType().equals(UserType.EMPLOYEE)) {
            return true;
        }
        return account.getUserId().equals(loggedInUser.getId());
    }

    private Account checkAndGetAccount(String iban, User loggedInUser) {
        if (iban.equals("NL01-INHO-0000-0000-01")) {
            throw new AccountNotAccessibleException("This is the bank's account, you cannot access this account");
        }

        //  Get the account
        Account account = accountRepository.getAccountByIban(iban).orElseThrow(() -> new AccountNotFoundException("Account with IBAN: " + iban + " does not exist"));

        //  Check if the account belongs to the user or if the user is an employee
        if (!checkIfAccountBelongsToUser(account, loggedInUser)) {
            throw new AccountNotAccessibleException("Account with IBAN: " + iban + " does not belong to the logged in user");
        }

        return account;
    }

    public List<Account> getAllAccounts(String search, Boolean active, User loggedInUser) {
        // Check if the user is an employee
        if (loggedInUser.getUserType().getAuthority().equals("EMPLOYEE")) {
            //  Get all accounts
            return accountRepository.findAllBySearchTerm(search, active);
        } else {
            //  Get all accounts of the user
            return accountRepository.findAllBySearchTermAndUserId(search, active, loggedInUser.getId());
        }
    }

    public AccountData getAccountByIban(String iban, User loggedInUser) {
        //  Check account and get the account
        Account account = this.checkAndGetAccount(iban, loggedInUser);

        //  Get the account limits left
        AccountLimitsLeft accountLimitsLeft = new AccountLimitsLeft();
        Double doubleValue = transactionService.getTodaysAccumulatedTransactionAmount(account.getIban());
        Float floatValue = doubleValue != null ? doubleValue.floatValue() : 0F;

        //  Set the account limits left
        accountLimitsLeft.setDailyLimitLeft(account.getDailyLimit() - floatValue);
        accountLimitsLeft.setTransactionLimit(account.getTransactionLimit());

        //  The amount spendable on the next transaction is the minimum of the daily limit left, the transaction limit and the balance minus the absolute limit
        accountLimitsLeft.setDifferenceBalanceAndAbsoluteLimit(account.getBalance() - account.getAbsoluteLimit());
        accountLimitsLeft.setAmountSpendableOnNextTransaction(Math.min(Math.min(accountLimitsLeft.getDailyLimitLeft(), accountLimitsLeft.getTransactionLimit()), accountLimitsLeft.getDifferenceBalanceAndAbsoluteLimit()));

        //  Return an AccountData object which contains the account (converted to an AccountResponseDTO object) and the account limits left
        return new AccountData(new AccountResponseDTO(account), accountLimitsLeft);
    }

    public List<Account> getAllAccountsByUserId(Long userId, User loggedInUser) {
        //  Check if the userId matches the id of the logged-in user and throw an exception if it doesn't unless the user is an employee
        if (!loggedInUser.getUserType().getAuthority().equals("EMPLOYEE") && !userId.equals(loggedInUser.getId())) {
            throw new AccountNotAccessibleException("You cannot access the accounts of another user");
        }
        return accountRepository.findAllByUserId(userId);
    }

    public Account update(AccountRequestDTO account, User loggedInUser) {
        // Check if the accountRequestDTO is valid
        this.checkIfAccountRequestDTOIsValidForUpdate(account);

        // Check if the account exists and get the account
        Account accountToUpdate = checkAndGetAccount(account.getIban(), loggedInUser);

        // Update the account
        Account updatedAccount = getUpdatedAccount(account, accountToUpdate);

        // Save the account
        accountRepository.save(updatedAccount);

        // Create a response object and return it
        return updatedAccount;
    }
    private Account getUpdatedAccount(AccountRequestDTO accountWithNewValues, Account accountToUpdate) {
        //  Update all the fields unless it's null
        //  Except for iban, userId and balance
        if(accountWithNewValues.getBalance() != null){
            if (!accountWithNewValues.getBalance().equals(accountToUpdate.getBalance())){
                throw new AccountUpdateException("You cannot update the balance of an account");
            }
        }

        Optional.ofNullable(accountWithNewValues.getDailyLimit()).ifPresent(accountToUpdate::setDailyLimit);
        Optional.ofNullable(accountWithNewValues.getTransactionLimit()).ifPresent(accountToUpdate::setTransactionLimit);
        Optional.ofNullable(accountWithNewValues.getAbsoluteLimit()).ifPresent(accountToUpdate::setAbsoluteLimit);
        Optional.ofNullable(accountWithNewValues.getName()).ifPresent(accountToUpdate::setName);
        Optional.ofNullable(accountWithNewValues.getIsSavings()).ifPresent(accountToUpdate::setIsSavings);

        // Return the updated account
        return accountToUpdate;
    }

    public String delete(String iban, User loggedInUser) {

        // Check account and get the account
        Account account = this.checkAndGetAccount(iban, loggedInUser);

        // Check if the account is active
        if (!account.getIsActive()) {
            throw new AccountCannotBeDeletedException("Account with IBAN: " + iban + " is already inactive");
        }

        // Delete the account
        account.setIsActive(false);
        accountRepository.save(account);

        return "Account with IBAN: " + iban + " has been set to inactive";
    }

    public void addSeededAccount(String iban, Account account) {
        account.setIban(iban);
        accountRepository.save(account);
        //System.out.println("Account with IBAN: " + iban + " has been added");
    }
}
