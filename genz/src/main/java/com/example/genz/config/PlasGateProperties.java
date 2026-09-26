package com.example.genz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plasgate")
public record PlasGateProperties(
    String baseUrl,
    String privateKey,
    String secretKey, 
    String sender
) {
    public PlasGateProperties {
        baseUrl = clean(baseUrl);
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://cloudapi.plasgate.com";
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        privateKey = clean(privateKey);
        secretKey = clean(secretKey);
        sender = clean(sender);
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("'") && trimmed.endsWith("'"))
                || (trimmed.startsWith("\"") && trimmed.endsWith("\""))) {
            if (trimmed.length() >= 2) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed;
    }
}

