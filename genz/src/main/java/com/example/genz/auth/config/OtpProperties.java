package com.example.genz.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "otp")
public class OtpProperties {
    private int expirationMinutes = 5;
    private int maxAttempts = 5;
    private int resendCooldownSeconds = 60;
    private int length = 6;
    private boolean consoleDeliveryEnabled = true;
}
