package com.example.genz.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Shared expiring cache. Redis failures never prevent the underlying operation. */
@Component
public class BackendResponseCache {
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final boolean enabled;
    private final ConcurrentHashMap<String, CompletableFuture<Object>> pending = new ConcurrentHashMap<>();
    private volatile long retryRedisAt;

    public BackendResponseCache(StringRedisTemplate redis, ObjectMapper mapper,
            @Value("${backend.cache.enabled:true}") boolean enabled) {
        this.redis = redis; this.mapper = mapper; this.enabled = enabled;
    }

    public static String key(String namespace, String... parts) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
                digest.update((bytes.length + ":").getBytes(StandardCharsets.UTF_8));
                digest.update(bytes);
            }
            return "nextgoal:cache:v1:" + namespace + ":" + HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public <T> T get(String key, Class<T> type, Duration ttl, Supplier<T> load) {
        if (!enabled || ttl.isZero() || ttl.isNegative()) return load.get();
        T cached = read(key, type);
        if (cached != null) return cached;
        // Bound coalescing state; Redis entries themselves always have a TTL.
        if (pending.size() >= 256) return load.get();
        var future = new CompletableFuture<Object>();
        var existing = pending.putIfAbsent(key, future);
        if (existing != null) {
            try { return type.cast(existing.join()); }
            catch (CompletionException ex) {
                if (ex.getCause() instanceof RuntimeException cause) throw cause;
                if (ex.getCause() instanceof Error cause) throw cause;
                throw ex;
            }
        }
        try {
            // Another request may have populated Redis since the first lookup.
            cached = read(key, type);
            T result = cached != null ? cached : load.get();
            if (cached == null && result != null) write(key, result, ttl);
            future.complete(result);
            return result;
        } catch (RuntimeException | Error ex) {
            future.completeExceptionally(ex);
            throw ex;
        } finally {
            pending.remove(key, future);
        }
    }

    private <T> T read(String key, Class<T> type) {
        if (System.nanoTime() < retryRedisAt) return null;
        final String json;
        try { json = redis.opsForValue().get(key); }
        catch (RuntimeException ex) { unavailable(); return null; }
        if (json == null) return null;
        try { return mapper.readValue(json, type); }
        catch (Exception ex) { return null; }
    }

    private void write(String key, Object value, Duration ttl) {
        if (System.nanoTime() < retryRedisAt) return;
        final String json;
        try { json = mapper.writeValueAsString(value); }
        catch (Exception ex) { return; }
        if (json.length() > 131072) return;
        try { redis.opsForValue().set(key, json, ttl); }
        catch (RuntimeException ex) { unavailable(); }
    }

    public void evict(String key) {
        if (!enabled) return;
        try { redis.delete(key); }
        catch (RuntimeException ex) { unavailable(); }
    }

    private void unavailable() {
        retryRedisAt = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    }
}
