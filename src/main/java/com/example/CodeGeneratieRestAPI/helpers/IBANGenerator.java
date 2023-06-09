package com.example.CodeGeneratieRestAPI.helpers;

import com.example.CodeGeneratieRestAPI.exceptions.IBANGenerationException;
import com.example.CodeGeneratieRestAPI.models.Account;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Random;

@Service
public class IBANGenerator {
    private static final BigInteger MODULUS = BigInteger.valueOf(97);
    private final int IBAN_LENGTH = 18; //   18 is the standard length of an IBAN in the Netherlands + all the dashes is 22
    private final String COUNTRY_CODE = "NL";
    private final int CHECKSUM_LENGTH = 2;
    private final String BANK_CODE = "INHO"; // This is the bank code for the bank that is used in this project MRBA
    private ServiceHelper serviceHelper;

    public IBANGenerator(ServiceHelper serviceHelper) {
        this.serviceHelper = serviceHelper;
    }

    // Calculate the two-digit checksum using modulo-97 (this is the ISO 7064 mod 97-10 algorithm)
    private static int calculateChecksum(String ibanDigits) {
        ibanDigits += "00"; // Append the country code and checksum (initially 00)
        BigInteger number = new BigInteger(ibanDigits);
        BigInteger mod97 = number.mod(new BigInteger(MODULUS.toByteArray()));
        return 98 - mod97.intValue(); // Subtract the remainder from 98
    }

    public String getUniqueIban() {
        do {
            String iban = generateIban();
            if (!serviceHelper.checkIfObjectExistsByIdentifier(iban, new Account())) {
                return iban;
            }
        } while (true);
    }

    public String generateIban() {
        try {
            StringBuilder ibanBuilder = new StringBuilder();
            Random random = new Random();

            ibanBuilder.append("0"); // The first number must be 0 according to user story 3
            // Generate a string of random numbers
            for (int i = 0; i < IBAN_LENGTH - COUNTRY_CODE.length() - BANK_CODE.length() - CHECKSUM_LENGTH - 1; i++) { // -1 because the first number must be 0
                ibanBuilder.append(random.nextInt(10));
            }

            // Calculate the two-digit checksum
            String ibanDigits = ibanBuilder.toString();
            int checksum = calculateChecksum(ibanDigits);

            // Format the IBAN with the checksum and components
            StringBuilder formattedIban = new StringBuilder(COUNTRY_CODE);
            formattedIban.append(String.format("%02d", checksum)).append(BANK_CODE).append(ibanDigits);

            // Insert dashes every 4 characters for readability
            for (int i = 4; i < formattedIban.length(); i += 5) {
                formattedIban.insert(i, "-");
            }

            // Return the formatted IBAN
            // IMPORTANT! This could return an already existing IBAN,
            // so make sure to check if the IBAN already exists in the database before saving it
            // OR use the getUniqueIban method
            return formattedIban.toString().toUpperCase();
        } catch (Exception exception) {
            throw new IBANGenerationException(exception.getMessage(), exception);
        }

    }
}
