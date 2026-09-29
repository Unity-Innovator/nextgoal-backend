package com.example.genz.assistant;

import com.example.genz.common.cache.BackendResponseCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BackendCacheTests {
    private final Map<String, String> values = new ConcurrentHashMap<>();
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);
    private BackendResponseCache cache() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenAnswer(c -> values.get(c.getArgument(0)));
        doAnswer(c -> { values.put(c.getArgument(0), c.getArgument(1)); return null; })
                .when(ops).set(anyString(), anyString(), any(Duration.class));
        return new BackendResponseCache(redis, new ObjectMapper(), true);
    }
    @Test void sharedSerializationTtlAndExpiry() {
        var first = cache();
        var second = new BackendResponseCache(redis, new ObjectMapper(), true);
        String key = BackendResponseCache.key("test", "private-user", "private-question");
        assertFalse(key.contains("private"));
        var reply = new AssistantService.Reply("Answer", List.of(new KnowledgeBase.Source("1", "Title", "Text")));
        assertEquals(reply, first.get(key, AssistantService.Reply.class, Duration.ofMinutes(2), () -> reply));
        assertEquals(reply, second.get(key, AssistantService.Reply.class, Duration.ofMinutes(2), () -> { fail("cache miss"); return null; }));
        verify(ops).set(eq(key), anyString(), eq(Duration.ofMinutes(2)));
        values.clear(); // Redis expiry is represented by an absent entry.
        assertEquals("fresh", second.get(key, String.class, Duration.ofMinutes(2), () -> "fresh"));
    }
    @Test void failuresAreNotCachedAndRedisOutageFallsBack() {
        var cache = cache();
        assertThrows(IllegalStateException.class, () -> cache.get("a", String.class, Duration.ofMinutes(2), () -> { throw new IllegalStateException(); }));
        assertEquals("ok", cache.get("a", String.class, Duration.ofMinutes(2), () -> "ok"));
        when(ops.get("b")).thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("offline"));
        assertEquals("live", cache.get("b", String.class, Duration.ofMinutes(2), () -> "live"));
        assertEquals("live again", cache.get("c", String.class, Duration.ofMinutes(2), () -> "live again"));
        verify(ops, never()).get("c");
    }
    @Test void userIsolationAndDocumentInvalidation() {
        var cache = cache();
        var store = mock(RagStore.class); var provider = mock(RagProvider.class);
        when(provider.cacheModel()).thenReturn("model");
        when(store.list(anyString())).thenReturn(List.of(new RagStore.Document(UUID.randomUUID(), "doc", "txt", 1)));
        when(store.cacheRevision(anyString())).thenReturn("version-one");
        when(provider.embedQuery(anyString())).thenReturn("vector");
        var sources = List.of(new KnowledgeBase.Source("1", "Doc", "Private content"));
        when(store.search(anyString(), eq("vector"), eq(0.3))).thenReturn(sources);
        when(provider.answer(anyString(), eq(sources))).thenReturn("reply");
        var service = new RagService(store, provider, new DocumentExtractor(), 0.3);
        service.configureCache(cache, Duration.ofMinutes(2));
        service.answer("alice", "question"); service.answer("alice", "question");
        verify(provider, times(1)).embedQuery(anyString());
        service.answer("bob", "question");
        verify(provider, times(2)).embedQuery(anyString());
        when(store.cacheRevision("alice")).thenReturn("version-two");
        service.answer("alice", "question");
        verify(provider, times(3)).embedQuery(anyString());
        when(store.list("alice")).thenReturn(List.of());
        assertTrue(service.answer("alice", "question").sources().isEmpty());
    }
    @Test void concurrentMissesShareOneProviderCall() throws Exception {
        var cache = cache(); var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var calls = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<String> request = () -> cache.get("same", String.class, Duration.ofMinutes(2), () -> {
                calls.incrementAndGet(); entered.countDown();
                try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException ex) { throw new RuntimeException(ex); }
                return "reply";
            });
            var first = executor.submit(request); assertTrue(entered.await(5, TimeUnit.SECONDS));
            var second = executor.submit(request); release.countDown();
            assertEquals("reply", first.get(5, TimeUnit.SECONDS));
            assertEquals("reply", second.get(5, TimeUnit.SECONDS)); assertEquals(1, calls.get());
        }
    }
    @Test void disabledCacheDoesNotContactRedis() {
        var cache = new BackendResponseCache(redis, new ObjectMapper(), false);
        assertEquals("reply", cache.get("key", String.class, Duration.ofMinutes(2), () -> "reply"));
        verifyNoInteractions(redis);
    }
}
