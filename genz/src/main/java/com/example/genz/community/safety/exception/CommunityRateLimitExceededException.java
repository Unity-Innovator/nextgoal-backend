package com.example.genz.community.safety.exception;

public class CommunityRateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public CommunityRateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
