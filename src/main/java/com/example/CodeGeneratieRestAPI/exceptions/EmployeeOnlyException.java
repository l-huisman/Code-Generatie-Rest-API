package com.example.CodeGeneratieRestAPI.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "You are not an employee")
public class EmployeeOnlyException extends RuntimeException{
    public EmployeeOnlyException(String message) {
        super(message);
    }
}
