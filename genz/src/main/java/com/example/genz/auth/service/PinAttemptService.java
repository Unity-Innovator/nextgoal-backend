package com.example.genz.auth.service;

import com.example.genz.common.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PinAttemptService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long LOCKOUT_DURATION_SECONDS = 15 * 60; // 15 minutes

    private final Clock clock;
    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public PinAttemptService() {
        this(Clock.systemUTC());
    }

    public PinAttemptService(Clock clock) {
        this.clock = clock;
    }

    public void checkAttempts(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }

        AttemptState state = attempts.get(phoneNumber);
        if (state == null) {
            return;
        }

        Instant now = clock.instant();
        if (state.lockedUntil != null) {
            if (now.isBefore(state.lockedUntil)) {
                throw new BadRequestException("Too many failed PIN attempts. Please try again later.");
            } else {
                attempts.remove(phoneNumber);
            }
        }
    }

    public void recordFailedAttempt(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }

        Instant now = clock.instant();
        attempts.compute(phoneNumber, (key, state) -> {
            if (state == null || (state.lockedUntil != null && now.isAfter(state.lockedUntil))) {
                state = new AttemptState();
            }

            state.failedCount++;
            state.lastAttemptTime = now;

            if (state.failedCount >= MAX_FAILED_ATTEMPTS) {
                state.lockedUntil = now.plusSeconds(LOCKOUT_DURATION_SECONDS);
            }

            return state;
        });
    }

    public void resetAttempts(String phoneNumber) {
        if (phoneNumber != null) {
            attempts.remove(phoneNumber);
        }
    }

    public boolean isLocked(String phoneNumber) {
        AttemptState state = attempts.get(phoneNumber);
        if (state == null || state.lockedUntil == null) {
            return false;
        }
        return clock.instant().isBefore(state.lockedUntil);
    }

    public int getFailedAttempts(String phoneNumber) {
        AttemptState state = attempts.get(phoneNumber);
        return state != null ? state.failedCount : 0;
    }

    private static class AttemptState {
        int failedCount = 0;
        Instant lockedUntil = null;
        Instant lastAttemptTime = null;
    }
}
