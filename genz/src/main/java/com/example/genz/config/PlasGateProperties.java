package com.example.genz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plasgate")
public record  PlasGateProperties(
    String baseUrl,
    String privateKey,
    String secretKey, 
    String sender
) {

}
