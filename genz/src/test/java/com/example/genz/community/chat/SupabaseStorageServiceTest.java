package com.example.genz.community.chat;

import com.example.genz.community.chat.enums.AttachmentType;
import com.example.genz.community.chat.service.SupabaseStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SupabaseStorageServiceTest {
    @Test
    void createsNewAttachmentWithPostAndPreservesAudioContentType() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<byte[]> body = new AtomicReference<>();
        server.createContext("/storage/v1/object/chat/voice.m4a", exchange -> {
            method.set(exchange.getRequestMethod());
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            body.set(exchange.getRequestBody().readAllBytes());
            // A new object must use the create endpoint, not the update operation.
            exchange.sendResponseHeaders("POST".equals(method.get()) ? 200 : 404, -1);
            exchange.close();
        });
        server.start();
        try {
            SupabaseStorageService service = new SupabaseStorageService(
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "test-key", "chat", new ObjectMapper());
            byte[] bytes = "audio fixture".getBytes(StandardCharsets.UTF_8);
            service.upload(new MockMultipartFile("file", "voice.m4a", "audio/mp4", bytes),
                    "voice.m4a", AttachmentType.VOICE);
            assertEquals("POST", method.get());
            assertEquals("audio/mp4", contentType.get());
            assertArrayEquals(bytes, body.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void cachesSignedUrlsAndEvictsWhenDeleting() throws Exception {
        var redis = org.mockito.Mockito.mock(org.springframework.data.redis.core.StringRedisTemplate.class);
        var ops = (org.springframework.data.redis.core.ValueOperations<String, String>)
                org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        var values = new java.util.HashMap<String, String>();
        org.mockito.Mockito.when(redis.opsForValue()).thenReturn(ops);
        org.mockito.Mockito.when(ops.get(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(c -> values.get(c.getArgument(0)));
        org.mockito.Mockito.doAnswer(c -> { values.put(c.getArgument(0), c.getArgument(1)); return null; })
                .when(ops).set(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.eq(java.time.Duration.ofMinutes(5)));
        org.mockito.Mockito.when(redis.delete(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(c -> values.remove(c.getArgument(0)) != null);
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/storage/v1/object/sign/chat/clip.mp4", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] bytes = ("{\"signedURL\":\"/object/sign/chat/clip.mp4?token=" + calls.incrementAndGet() + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.createContext("/storage/v1/object/chat/clip.mp4", exchange -> {
            exchange.sendResponseHeaders(200, -1); exchange.close();
        });
        server.start();
        try {
            var service = new SupabaseStorageService("http://127.0.0.1:" + server.getAddress().getPort(),
                    "test-key", "chat", new ObjectMapper());
            service.configureCache(new com.example.genz.common.cache.BackendResponseCache(redis, new ObjectMapper(), true));
            String first = service.createSignedUrl("clip.mp4");
            assertEquals(first, service.createSignedUrl("clip.mp4"));
            assertEquals(1, calls.get());
            service.deleteQuietly("clip.mp4");
            service.createSignedUrl("clip.mp4");
            assertEquals(2, calls.get());
        } finally { server.stop(0); }
    }
}
