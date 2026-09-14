package com.example.genz.community.chat.exception;

public class ChatStorageException extends RuntimeException {

    public ChatStorageException(String message) {
        super(message);
    }

    public ChatStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
