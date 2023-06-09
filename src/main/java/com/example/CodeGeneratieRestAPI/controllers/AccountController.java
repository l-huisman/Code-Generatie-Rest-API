package com.example.CodeGeneratieRestAPI.controllers;

import com.example.CodeGeneratieRestAPI.dtos.AccountData;
import com.example.CodeGeneratieRestAPI.dtos.AccountRequestDTO;
import com.example.CodeGeneratieRestAPI.dtos.AccountResponseDTO;
import com.example.CodeGeneratieRestAPI.exceptions.*;
import com.example.CodeGeneratieRestAPI.helpers.LoggedInUserHelper;
import com.example.CodeGeneratieRestAPI.models.Account;
import com.example.CodeGeneratieRestAPI.models.ApiResponse;
import com.example.CodeGeneratieRestAPI.models.User;
import com.example.CodeGeneratieRestAPI.services.AccountService;
import jakarta.annotation.Nullable;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.config.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final ModelMapper modelMapper;
    @Autowired
    private final LoggedInUserHelper loggedInUserHelper;
    @Autowired
    private AccountService accountService;

    public AccountController() {
        this.loggedInUserHelper = new LoggedInUserHelper();
        modelMapper = new ModelMapper();

        //  Set the field matching to strict
        modelMapper.getConfiguration().setFieldMatchingEnabled(true).setFieldAccessLevel(Configuration.AccessLevel.PRIVATE);
        PropertyMap<Account, AccountResponseDTO> accountMap = new PropertyMap<>() {
            protected void configure() {
                map().setUserId(source.getUser().getId());
            }
        };
        modelMapper.addMappings(accountMap);

    }

    //  POST mappings
    @PostMapping
    public ResponseEntity<ApiResponse> add(@RequestBody(required = true) AccountRequestDTO accountRequestDTO) {
        try {
            //  Get the logged-in user
            User user = loggedInUserHelper.getLoggedInUser();

            //  Retrieve the data
            Account account = accountService.add(accountRequestDTO, user);

            //  Return the data
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Account created successfully", new AccountResponseDTO(account)));
        } catch (UserNotFoundException | AccountCreationException | IBANGenerationException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    //  GET mappings
    @GetMapping()
    public ResponseEntity<ApiResponse> getAllAccounts(@RequestParam(required = false) String search, @Nullable @RequestParam(required = false, defaultValue = "") Boolean active) {
        try {
            //  Get the logged-in user
            User user = loggedInUserHelper.getLoggedInUser();

            //  Retrieve the data
            List<Account> accounts = accountService.getAllAccounts(search, active, user);

            //  Return the data
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse(true, accounts.stream().count() + " Accounts retrieved", Arrays.asList(modelMapper.map(accounts, AccountResponseDTO[].class))));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getAllAccountsByUserId(@PathVariable(required = true) Long userId) {
        try {
            //  Get the logged-in user
            User user = loggedInUserHelper.getLoggedInUser();

            //  Retrieve the data
            List<Account> accounts = accountService.getAllAccountsByUserId(userId, user);

            //  Return the data
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse(true, accounts.stream().count() + " Accounts retrieved", Arrays.asList(modelMapper.map(accounts, AccountResponseDTO[].class))));
        } catch (AccountNotFoundException | UserNotFoundException | AccountNotAccessibleException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @GetMapping("/{iban}")
    public ResponseEntity<ApiResponse> getAccountByIban(@PathVariable(required = true) String iban) {

        try {
            //  Get the logged-in user
            User user = loggedInUserHelper.getLoggedInUser();

            //  Retrieve the data
            AccountData accountData = accountService.getAccountByIban(iban, user);

            //  Return the data
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(true, "Account retrieved successfully", accountData));
        } catch (AccountNotFoundException | UserNotFoundException | AccountNotAccessibleException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }
    // PUT mappings

    @PutMapping()
    public ResponseEntity<ApiResponse> update(@RequestBody(required = true) AccountRequestDTO accountRequestDTO) {
        try {
            //  Get the logged-in user
            User user = loggedInUserHelper.getLoggedInUser();

            //  Retrieve the data
            Account account = accountService.update(accountRequestDTO, user);

            //  Return the data
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(true, "Account updated successfully", new AccountResponseDTO(account)));
        } catch (AccountNoDataChangedException | UserNotFoundException | AccountUpdateException |
                 AccountNotAccessibleException | AccountNotFoundException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }
    //  DELETE mappings

    //  This is a SOFT delete, HARD deletes are NOT allowed
    @DeleteMapping("/{iban}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable(required = true) String iban) {
        try {
            //  Get the logged-in user
            User user = loggedInUserHelper.getLoggedInUser();

            //  Perform the delete
            String responseBody = accountService.delete(iban, user);

            // Return a response entity with the response body and the status code
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(true, responseBody));

        } catch (AccountNotFoundException | UserNotFoundException | AccountNotAccessibleException |
                 AccountCannotBeDeletedException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }
}
