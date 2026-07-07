package com.example.genz.common.exception;

public class EmailAlreadyExistException extends RuntimeException{
    public EmailAlreadyExistException(String message){
        super(message);
    }
}

