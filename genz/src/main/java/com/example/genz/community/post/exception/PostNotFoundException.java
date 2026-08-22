package com.example.genz.community.post.exception;

public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(String message) {
        super(message);
    }
}
