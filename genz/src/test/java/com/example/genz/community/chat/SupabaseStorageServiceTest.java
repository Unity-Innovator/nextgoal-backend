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
}
