package com.example.genz.auth.service;

import com.example.genz.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class PinAttemptServiceTest {

    private MutableClock clock;
    private PinAttemptService pinAttemptService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-09-03T12:00:00Z"));
        pinAttemptService = new PinAttemptService(clock);
    }

    @Test
    void allowsAttemptsBelowLimit() {
        String phone = "0123456789";

        pinAttemptService.checkAttempts(phone);
        pinAttemptService.recordFailedAttempt(phone);
        pinAttemptService.recordFailedAttempt(phone);

        assertEquals(2, pinAttemptService.getFailedAttempts(phone));
        assertFalse(pinAttemptService.isLocked(phone));
        assertDoesNotThrow(() -> pinAttemptService.checkAttempts(phone));
    }

    @Test
    void locksOutAfterMaxFailedAttempts() {
        String phone = "0123456789";

        for (int i = 0; i < PinAttemptService.MAX_FAILED_ATTEMPTS; i++) {
            pinAttemptService.recordFailedAttempt(phone);
        }

        assertTrue(pinAttemptService.isLocked(phone));
        BadRequestException ex = assertThrows(BadRequestException.class, () -> pinAttemptService.checkAttempts(phone));
        assertTrue(ex.getMessage().contains("Too many failed PIN attempts"));
    }

    @Test
    void resetAttemptsClearsLockoutAndCounter() {
        String phone = "0123456789";

        for (int i = 0; i < PinAttemptService.MAX_FAILED_ATTEMPTS; i++) {
            pinAttemptService.recordFailedAttempt(phone);
        }

        assertTrue(pinAttemptService.isLocked(phone));

        pinAttemptService.resetAttempts(phone);

        assertFalse(pinAttemptService.isLocked(phone));
        assertEquals(0, pinAttemptService.getFailedAttempts(phone));
        assertDoesNotThrow(() -> pinAttemptService.checkAttempts(phone));
    }

    @Test
    void lockoutExpiresAfterDuration() {
        String phone = "0123456789";

        for (int i = 0; i < PinAttemptService.MAX_FAILED_ATTEMPTS; i++) {
            pinAttemptService.recordFailedAttempt(phone);
        }

        assertTrue(pinAttemptService.isLocked(phone));

        // Advance time past lockout duration (15 minutes + 1 second)
        clock.advanceSeconds(PinAttemptService.LOCKOUT_DURATION_SECONDS + 1);

        assertFalse(pinAttemptService.isLocked(phone));
        assertDoesNotThrow(() -> pinAttemptService.checkAttempts(phone));
    }

    private static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneOffset zone = ZoneOffset.UTC;

        public MutableClock(Instant instant) {
            this.instant = instant;
        }

        public void advanceSeconds(long seconds) {
            this.instant = this.instant.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return zone;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
