package com.example.genz.assistant;

import java.net.SocketTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/** Only fixed diagnostic codes and numeric HTTP status are logged, never payloads or credentials. */
final class AssistantDiagnostics {
    private static final Logger log = LoggerFactory.getLogger(AssistantDiagnostics.class);

    private AssistantDiagnostics() {}

    static AssistantService.Unavailable unavailable(String provider, String operation, String reason) {
        log.warn("Assistant provider={} operation={} reason={}", provider, operation, reason);
        return new AssistantService.Unavailable();
    }

    static AssistantService.Unavailable failure(String provider, String operation, RuntimeException error) {
        if (error instanceof AssistantService.Unavailable unavailable) return unavailable;
        if (error instanceof RestClientResponseException response) {
            int status = response.getStatusCode().value();
            String reason = switch (status) {
                case 400 -> "INVALID_PROVIDER_REQUEST";
                case 401, 403 -> "PROVIDER_AUTH_OR_PERMISSION";
                case 404 -> "MODEL_OR_ENDPOINT_NOT_FOUND";
                case 429 -> "PROVIDER_RATE_LIMIT_OR_QUOTA";
                default -> status >= 500 ? "PROVIDER_SERVER_ERROR" : "PROVIDER_HTTP_ERROR";
            };
            log.warn("Assistant provider={} operation={} reason={} status={}", provider, operation, reason, status);
            return new AssistantService.Unavailable();
        }
        if (error instanceof ResourceAccessException) {
            Throwable cause = error;
            while (cause != null) {
                if (cause instanceof SocketTimeoutException) {
                    return unavailable(provider, operation, "PROVIDER_TIMEOUT");
                }
                cause = cause.getCause();
            }
            return unavailable(provider, operation, "PROVIDER_CONNECTION_ERROR");
        }
        return unavailable(provider, operation, "PROVIDER_RESPONSE_ERROR");
    }
}
