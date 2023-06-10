package com.example.CodeGeneratieRestAPI.controllers;

import com.example.CodeGeneratieRestAPI.controllers.TransactionController;
import com.example.CodeGeneratieRestAPI.helpers.ServiceHelper;
import com.example.CodeGeneratieRestAPI.jwt.JwTokenFilter;
import com.example.CodeGeneratieRestAPI.jwt.JwTokenProvider;
import com.example.CodeGeneratieRestAPI.models.*;
import com.example.CodeGeneratieRestAPI.repositories.UserRepository;
import com.example.CodeGeneratieRestAPI.services.AccountService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.example.CodeGeneratieRestAPI.services.TransactionService;

import javax.swing.text.html.Option;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// IntelliJ probably already loads the spring context
// Just in case, we use @ExtendWith to ensure the context is loaded.
// We use @WebMvcTest because it allows us to only test the controller
// and not load in anything else (repositories, services etc.)
@ExtendWith(SpringExtension.class)
@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc
class TransactionControllerTest {

    // We use a unit test for controller methods to test any custom logic we have in there
    // In this case, we're using ModelMapper and we have to check if this produces the correct results


    // We use mockMvc to simulate HTTP requests to a controller class
    @Autowired
    private MockMvc mockMvc;

    // We mock our service, because we don't want to test it here
    // Note that we have to Mock all dependencies our controller code uses if we use @WebMvcTest
    @MockBean
    private TransactionService transactionService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwTokenProvider jwTokenProvider;

    private ServiceHelper serviceHelper;

    // We could also add ObjectMapper to convert objects to JSON for us

    @BeforeEach
    void setUp() {
        ServiceHelper serviceHelper = new ServiceHelper();
        serviceHelper.setUserRepository(userRepository);
    }

    private User getMockUser(Long id, UserType userType, String username) {
        User user = new User();
        user.setId(id);
        user.setUserType(userType);
        user.setUsername(username);
        user.setPassword(new HashedPassword("john"));
        return user;
    }

    private Account getMockAccount(String iban, Float balance, User user, Boolean isSavings) {
        Account account = new Account();
        account.setIban(iban);
        account.setUser(user);
        account.setBalance(balance);
        account.setAbsoluteLimit(10F);
        account.setDailyLimit(200F);
        account.setTransactionLimit(100F);
        account.setIsSavings(isSavings);
        return account;
    }

    private Transaction getMockTransaction(Long id, User user, Float amount, TransactionType transactionType, Account fromAccount, Account toAccount) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setAmount(amount);
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setTransactionType(transactionType);
        return transaction;
    }

    @Test
    @WithMockUser(username = "Devon", password = "pwd", roles = "USER")
    void getAll() throws Exception {
        User user = getMockUser(1L, UserType.USER, "john");
        Account fromAccount = getMockAccount("123456", 1000F, user, false);
        
        LocalDate today = LocalDate.now();
        Date startDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        String search = "";
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(getMockTransaction(1L, user, 60F, TransactionType.WITHDRAW, fromAccount, null));
        transactions.add(getMockTransaction(2L, user, 60F, TransactionType.WITHDRAW, fromAccount, null));

        when(transactionService.getAll(user, startDate, endDate, search)).thenReturn(transactions);
        when(userRepository.findUserByUsername(anyString())).thenReturn(Optional.of(user));

        SimpleDateFormat DateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // Check if we get a 200 OK
        // And if the JSON content matches our expectations
        this.mockMvc.perform(get("/transactions?start_date=" + DateFormat.format(startDate) + "&end_date=" + DateFormat.format(endDate) + "&search=").header("Authorization", "test")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(transactions.size())))
                .andExpect(jsonPath("$.data[0].amount").value("60.0"));
    }

    @Test
    @WithMockUser(username = "Devon", password = "pwd", roles = "USER")
    void getAllByUserId() throws Exception {
        User user = getMockUser(1L, UserType.USER, "john");
        Account fromAccount = getMockAccount("123456", 1000F, user, false);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(getMockTransaction(1L, user, 60F, TransactionType.WITHDRAW, fromAccount, null));
        transactions.add(getMockTransaction(2L, user, 60F, TransactionType.WITHDRAW, fromAccount, null));

        when(transactionService.getAllByUser(user)).thenReturn(transactions);
        when(userRepository.findUserByUsername(anyString())).thenReturn(Optional.of(user));

        // Check if we get a 200 OK
        // And if the JSON content matches our expectations
        this.mockMvc.perform(get("/transactions/user").header("Authorization", "test")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(transactions.size())))
                .andExpect(jsonPath("$.data[0].amount").value("60.0"));
    }

    @Test
    @WithMockUser(username = "Devon", password = "pwd", roles = "USER")
    void getById() throws Exception {
        User user = getMockUser(1L, UserType.USER, "john");
        Account fromAccount = getMockAccount("123456", 1000F, user, false);

        Transaction transaction = getMockTransaction(1L, user, 60F, TransactionType.WITHDRAW, fromAccount, null);

        when(transactionService.getById(user, transaction.getId())).thenReturn(transaction);
        when(userRepository.findUserByUsername(anyString())).thenReturn(Optional.of(user));

        // Check if we get a 200 OK
        // And if the JSON content matches our expectations
        this.mockMvc.perform(get("/transactions/" + transaction.getId()).header("Authorization", "test")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value("60.0"));
    }

    @Test
    @WithMockUser(username = "Devon", password = "pwd", roles = "USER")
    void getAllByAccountIban() throws Exception {
        User user = getMockUser(1L, UserType.USER, "john");
        Account fromAccount = getMockAccount("123456", 1000F, user, false);

        LocalDate today = LocalDate.now();
        Date startDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        String search = "";
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(getMockTransaction(1L, user, 60F, TransactionType.WITHDRAW, fromAccount, null));
        transactions.add(getMockTransaction(2L, user, 60F, TransactionType.WITHDRAW, fromAccount, null));

        when(transactionService.getAllByAccountIban(user, fromAccount.getIban(), startDate, endDate, search)).thenReturn(transactions);
        when(userRepository.findUserByUsername(anyString())).thenReturn(Optional.of(user));

        SimpleDateFormat DateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // Check if we get a 200 OK
        // And if the JSON content matches our expectations
        this.mockMvc.perform(get("/transactions/accounts/" + fromAccount.getIban() + "?start_date=" + DateFormat.format(startDate) + "&end_date=" + DateFormat.format(endDate) + "&search=").header("Authorization", "test")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(transactions.size())))
                .andExpect(jsonPath("$.data[0].amount").value("60.0"));
    }

    // @Test
    // void add() throws Exception {

    //     // Arrange
    //     when(carService.add(any(Car.class))).thenReturn(new Car(2, "Mercedes", 2000, "CD4567", null));

    //     // Act & Assert
    //     this.mockMvc.perform(post("/cars")
    //                     .contentType(MediaType.APPLICATION_JSON_VALUE)
    //                     /// String literals in Java 17: enclose in """
    //                     .content("""
    //                              {
    //                                 "brand": "Mercedes",
    //                                 "weight": 2000,
    //                                 "licensePlate": "CD4567"
    //                               }
    //                             """))
    //             // But since we used any(Car.class) a simple {} should be enough
    //             .andDo(print())
    //             .andExpect(status().isCreated())
    //             .andExpect(jsonPath("$.brand").value("Mercedes"));
    // }
}