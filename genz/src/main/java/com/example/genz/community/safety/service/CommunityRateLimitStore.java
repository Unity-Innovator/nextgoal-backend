package com.example.genz.community.safety.service;

import com.example.genz.community.safety.enums.CommunityAction;

import java.util.UUID;

/**
 * Atomic counter storage for community rate limits. A Redis implementation can
 * replace the in-memory bean without changing callers or persistence services.
 */
public interface CommunityRateLimitStore {

    int incrementAndGet(
            UUID userId,
            CommunityAction action,
            long windowStartEpochSecond,
            long windowEndEpochSecond
    );
}
