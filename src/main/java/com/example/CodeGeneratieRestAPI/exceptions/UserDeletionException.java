package com.example.CodeGeneratieRestAPI.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "User could not be deleted, try again later or contact the administrator")
public class UserDeletionException extends RuntimeException implements CustomExceptionBase {
    public UserDeletionException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatusCode() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
