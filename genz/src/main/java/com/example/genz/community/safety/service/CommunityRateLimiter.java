package com.example.genz.community.safety.service;

import com.example.genz.community.safety.enums.CommunityAction;
import com.example.genz.community.safety.exception.CommunityRateLimitExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@Component
public class CommunityRateLimiter {

    private final CommunityRateLimitStore store;
    private final Clock clock;

    @Autowired
    public CommunityRateLimiter(CommunityRateLimitStore store) {
        this(store, Clock.systemUTC());
    }

    CommunityRateLimiter(Clock clock) {
        this(new InMemoryCommunityRateLimitStore(), clock);
    }

    CommunityRateLimiter(CommunityRateLimitStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public void check(UUID userId, CommunityAction action) {
        long now = clock.instant().getEpochSecond();
        long windowStart = now - Math.floorMod(now, action.windowSeconds());
        long windowEnd = windowStart + action.windowSeconds();
        int count = store.incrementAndGet(userId, action, windowStart, windowEnd);
        if (count > action.limit()) {
            long retryAfter = Math.max(1, windowEnd - now);
            throw new CommunityRateLimitExceededException(
                    "Too many " + action.name().toLowerCase().replace('_', ' ') + " requests",
                    retryAfter
            );
        }
    }
}
