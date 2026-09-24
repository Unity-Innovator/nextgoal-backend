package com.example.genz.community.safety.service;

import com.example.genz.community.safety.enums.CommunityAction;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryCommunityRateLimitStore implements CommunityRateLimitStore {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final ConcurrentHashMap<RateLimitKey, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public int incrementAndGet(
            UUID userId,
            CommunityAction action,
            long windowStartEpochSecond,
            long windowEndEpochSecond
    ) {
        RateLimitKey key = new RateLimitKey(userId, action);
        WindowCounter counter = counters.compute(key, (ignored, current) -> {
            int nextCount = current == null || current.windowStart() != windowStartEpochSecond
                    ? 1
                    : current.count() + 1;
            return new WindowCounter(windowStartEpochSecond, windowEndEpochSecond, nextCount);
        });

        if (counters.size() > CLEANUP_THRESHOLD) {
            counters.entrySet().removeIf(entry -> entry.getValue().windowEnd() <= windowStartEpochSecond);
        }
        return counter.count();
    }

    private record RateLimitKey(UUID userId, CommunityAction action) {
    }

    private record WindowCounter(long windowStart, long windowEnd, int count) {
    }
}
