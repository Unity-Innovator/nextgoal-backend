package com.example.genz.auth.service;

import com.example.genz.auth.config.OtpProperties;
import com.example.genz.auth.enums.OtpPurpose;
import com.example.genz.auth.exception.OtpException;
import com.example.genz.auth.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisOtpStore {
    public static final int TTL_SECONDS = 300;
    private final StringRedisTemplate redis;
    private final OtpProperties properties;

    // Both keys share a Redis Cluster hash slot. Resends replace the entire challenge.
    private static final DefaultRedisScript<Long> SAVE = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[2]) == 1 then return 0 end
            redis.call('HSET', KEYS[1], 'hash', ARGV[1], 'purpose', ARGV[2], 'attempts', 0)
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            redis.call('SET', KEYS[2], ARGV[1], 'EX', ARGV[4])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then return 0 end
            local attempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
            if redis.call('HGET', KEYS[1], 'hash') == ARGV[1]
                and redis.call('HGET', KEYS[1], 'purpose') == ARGV[2] then
                redis.call('DEL', KEYS[1])
                return 1
            end
            if attempts >= tonumber(ARGV[3]) then
                redis.call('DEL', KEYS[1])
                return -2
            end
            return -1
            """, Long.class);

    private static final DefaultRedisScript<Long> REMOVE = new DefaultRedisScript<>("""
            if redis.call('HGET', KEYS[1], 'hash') == ARGV[1] then
                redis.call('DEL', KEYS[1])
            end
            if redis.call('GET', KEYS[2]) == ARGV[1] then redis.call('DEL', KEYS[2]) end
            return 1
            """, Long.class);

    public void save(String phone, OtpPurpose purpose, String hash) {
        Long result = redis.execute(SAVE, keys(phone), hash, purpose.name(),
                String.valueOf(TTL_SECONDS), String.valueOf(properties.getResendCooldownSeconds()));
        if (!Long.valueOf(1).equals(result)) {
            throw new OtpException("Please wait before requesting a new OTP");
        }
    }

    public void consume(String phone, OtpPurpose purpose, String hash) {
        Long result = redis.execute(CONSUME, List.of(keys(phone).getFirst()), hash,
                purpose.name(), String.valueOf(properties.getMaxAttempts()));
        if (Long.valueOf(1).equals(result)) return;
        if (Long.valueOf(-2).equals(result)) throw new OtpException("Maximum OTP attempts exceeded");
        if (Long.valueOf(-1).equals(result)) throw new OtpException("Invalid OTP");
        throw new OtpException("OTP has expired or has already been used");
    }

    public void removeIfMatching(String phone, String hash) {
        redis.execute(REMOVE, keys(phone), hash);
    }

    private List<String> keys(String phone) {
        String prefix = "auth:otp:{" + TokenHashUtil.sha256Hex(phone) + "}";
        return List.of(prefix, prefix + ":cooldown");
    }
}
