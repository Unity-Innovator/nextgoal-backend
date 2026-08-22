package com.example.genz.community.safety.service;

import com.example.genz.community.safety.enums.CommunityAction;
import com.example.genz.community.safety.exception.CommunityRateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommunityRateLimiterTests {

    @Test
    void rejectsRequestsAboveTheActionLimitAndProvidesRetryAfter() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-22T00:00:30Z"), ZoneOffset.UTC);
        CommunityRateLimiter limiter = new CommunityRateLimiter(clock);
        UUID userId = UUID.randomUUID();
        for (int index = 0; index < CommunityAction.CREATE_POST.limit(); index++) {
            limiter.check(userId, CommunityAction.CREATE_POST);
        }

        CommunityRateLimitExceededException exception = assertThrows(
                CommunityRateLimitExceededException.class,
                () -> limiter.check(userId, CommunityAction.CREATE_POST)
        );
        assertEquals(30, exception.getRetryAfterSeconds());
    }
}
