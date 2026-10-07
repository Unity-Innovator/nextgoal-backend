package com.example.genz.assistant;

import java.net.SocketTimeoutException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(OutputCaptureExtension.class)
class AssistantDiagnosticsTests {
    @Test void documentStoreFailureIsDiagnosedWithoutSqlOrUserDetails(CapturedOutput output) {
        var store = org.mockito.Mockito.mock(RagStore.class);
        var provider = org.mockito.Mockito.mock(RagProvider.class);
        org.mockito.Mockito.when(store.cacheRevision("private-owner")).thenThrow(
                new org.springframework.dao.DataAccessResourceFailureException("private-sql"));
        var service = new RagService(store, provider, new DocumentExtractor(), 0.3);
        assertThrows(AssistantService.Unavailable.class, () -> service.answer("private-owner", "private-question"));
        assertTrue(output.getAll().contains("provider=rag operation=retrieval reason=DOCUMENT_STORE_ERROR"));
        assertFalse(output.getAll().contains("private-"));
    }

    @Test void providerHttpFailureLogsStatusWithoutSecrets(CapturedOutput output) {
        var builder = RestClient.builder().baseUrl("https://provider.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"secret-provider-body\"}"));
        var provider = new RagProvider("secret-api-key", "model", builder.build());
        var error = assertThrows(AssistantService.Unavailable.class,
                () -> provider.answer("private-question", List.of()));
        assertNull(error.getCause());
        assertTrue(output.getAll().contains("provider=gemini operation=generation"));
        assertTrue(output.getAll().contains("reason=PROVIDER_RATE_LIMIT_OR_QUOTA status=429"));
        for (String secret : List.of("secret-provider-body", "secret-api-key", "private-question")) {
            assertFalse(output.getAll().contains(secret));
        }
        server.verify();
    }

    @Test void missingKeyAndTruncatedResponseHaveDistinctDiagnostics(CapturedOutput output) {
        assertThrows(AssistantService.Unavailable.class,
                () -> new RagProvider("", "model", RestClient.create()).embedQuery("private-question"));
        var builder = RestClient.builder().baseUrl("https://provider.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withSuccess(
                "{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\"}]}", MediaType.APPLICATION_JSON));
        assertThrows(AssistantService.Unavailable.class,
                () -> new RagProvider("key", "model", builder.build()).answer("private-question", List.of()));
        assertTrue(output.getAll().contains("operation=embedding reason=MISSING_API_KEY"));
        assertTrue(output.getAll().contains("operation=generation reason=GENERATION_TOKEN_LIMIT"));
        assertFalse(output.getAll().contains("private-question"));
        server.verify();
    }

    @Test void timeoutLogsOnlySafeCategory(CapturedOutput output) {
        var error = new ResourceAccessException("private-url-and-key", new SocketTimeoutException("private-detail"));
        assertNotNull(AssistantDiagnostics.failure("groq", "generation", error));
        assertTrue(output.getAll().contains("reason=PROVIDER_TIMEOUT"));
        assertFalse(output.getAll().contains("private-"));
    }
}
