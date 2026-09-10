package com.example.genz.auth.service;

import com.example.genz.auth.config.OtpProperties;
import com.example.genz.auth.enums.OtpPurpose;
import com.example.genz.auth.exception.OtpException;
import com.example.genz.auth.util.TokenHashUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "OTP_TEST_REDIS_PORT", matches = "[0-9]+")
class RedisOtpStoreIntegrationTest {
    private static LettuceConnectionFactory connection;
    private static StringRedisTemplate redis;
    private RedisOtpStore store;
    private String phone;
    private String key;

    @BeforeAll
    static void connect() {
        connection = new LettuceConnectionFactory("127.0.0.1", Integer.parseInt(System.getenv("OTP_TEST_REDIS_PORT")));
        connection.afterPropertiesSet();
        redis = new StringRedisTemplate(connection);
    }

    @AfterAll
    static void disconnect() { connection.destroy(); }

    @BeforeEach
    void setUp() {
        phone = UUID.randomUUID().toString();
        key = "auth:otp:{" + TokenHashUtil.sha256Hex(phone) + "}";
        store = new RedisOtpStore(redis, new OtpProperties());
    }

    @AfterEach
    void cleanup() { redis.delete(java.util.List.of(key, key + ":cooldown")); }

    @Test
    void ttlIsFiveMinutesAndSuccessDeletesChallenge() {
        store.save(phone, OtpPurpose.REGISTRATION, "hash");
        assertTrue(redis.getExpire(key) > 295 && redis.getExpire(key) <= 300);
        assertEquals("hash", redis.opsForHash().get(key, "hash"));
        store.consume(phone, OtpPurpose.REGISTRATION, "hash");
        assertFalse(redis.hasKey(key));
        assertThrows(OtpException.class, () -> store.consume(phone, OtpPurpose.REGISTRATION, "hash"));
    }

    @Test
    void expiredCodeCannotBeConsumed() throws Exception {
        store.save(phone, OtpPurpose.REGISTRATION, "hash");
        redis.expire(key, Duration.ofMillis(1));
        Thread.sleep(20);
        assertThrows(OtpException.class, () -> store.consume(phone, OtpPurpose.REGISTRATION, "hash"));
    }

    @Test
    void fifthFailureDeletesCodeWithoutExtendingExpiry() {
        store.save(phone, OtpPurpose.REGISTRATION, "hash");
        redis.expire(key, Duration.ofSeconds(100));
        for (int i = 0; i < 4; i++) {
            assertThrows(OtpException.class, () -> store.consume(phone, OtpPurpose.REGISTRATION, "wrong"));
        }
        assertTrue(redis.getExpire(key) <= 100);
        assertTrue(redis.hasKey(key));
        assertThrows(OtpException.class, () -> store.consume(phone, OtpPurpose.PASSWORD_RESET, "hash"));
        assertFalse(redis.hasKey(key));
    }

    @Test
    void cooldownAndResendReplacementAndConditionalCleanup() {
        store.save(phone, OtpPurpose.REGISTRATION, "old");
        assertThrows(OtpException.class, () -> store.save(phone, OtpPurpose.REGISTRATION, "new"));
        redis.delete(key + ":cooldown");
        store.save(phone, OtpPurpose.REGISTRATION, "new");
        store.removeIfMatching(phone, "old");
        assertEquals("new", redis.opsForHash().get(key, "hash"));
        assertThrows(OtpException.class, () -> store.consume(phone, OtpPurpose.REGISTRATION, "old"));
        store.removeIfMatching(phone, "new");
        assertFalse(redis.hasKey(key));
        assertFalse(redis.hasKey(key + ":cooldown"));
        store.save(phone, OtpPurpose.REGISTRATION, "retry");
    }

    @Test
    void concurrentVerificationHasExactlyOneWinner() throws Exception {
        store.save(phone, OtpPurpose.REGISTRATION, "hash");
        try (var pool = Executors.newFixedThreadPool(8)) {
            CountDownLatch start = new CountDownLatch(1);
            var futures = new java.util.ArrayList<Future<Boolean>>();
            for (int i = 0; i < 8; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try { store.consume(phone, OtpPurpose.REGISTRATION, "hash"); return true; }
                    catch (OtpException ex) { return false; }
                }));
            }
            start.countDown();
            int successes = 0;
            for (var future : futures) if (future.get(5, TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes);
        }
    }
}
