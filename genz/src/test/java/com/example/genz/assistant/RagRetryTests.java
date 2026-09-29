package com.example.genz.assistant;

import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RagRetryTests {
    @Test void transientGenerationFailureRecoversOnRetry() {
        for (var status : List.of(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT)) {
            var builder = RestClient.builder().baseUrl("https://provider.test");
            var server = MockRestServiceServer.bindTo(builder).build();
            server.expect(anything()).andRespond(withStatus(status));
            server.expect(anything()).andRespond(withSuccess(
                    "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"Open Goals.\"}]}}]}",
                    MediaType.APPLICATION_JSON));
            var pauses = new ArrayList<Long>();
            var provider = new RagProvider("key", "model", builder.build(), pauses::add);
            assertEquals("Open Goals.", provider.answer("question", List.of()));
            assertEquals(1, pauses.size());
            assertTrue(pauses.getFirst() >= 750 && pauses.getFirst() <= 1250);
            server.verify();
        }
    }

    @Test void persistent503StopsAfterOneRetry() {
        var builder = RestClient.builder().baseUrl("https://provider.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        var provider = new RagProvider("key", "model", builder.build(), millis -> {});
        assertThrows(AssistantService.Unavailable.class, () -> provider.answer("question", List.of()));
        server.verify();
    }

    @Test void clientErrorsAreNotRetried() {
        for (var status : List.of(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN,
                HttpStatus.NOT_FOUND, HttpStatus.TOO_MANY_REQUESTS)) {
            var builder = RestClient.builder().baseUrl("https://provider.test");
            var server = MockRestServiceServer.bindTo(builder).build();
            server.expect(anything()).andRespond(withStatus(status));
            var provider = new RagProvider("key", "model", builder.build(), millis -> fail("Unexpected retry"));
            assertThrows(AssistantService.Unavailable.class, () -> provider.answer("question", List.of()));
            server.verify();
        }
    }

    @Test void interruptionStopsRetryAndPreservesInterruptFlag() {
        var builder = RestClient.builder().baseUrl("https://provider.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        var provider = new RagProvider("key", "model", builder.build(), millis -> { throw new InterruptedException(); });
        try {
            assertThrows(AssistantService.Unavailable.class, () -> provider.answer("question", List.of()));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        server.verify();
    }
}
