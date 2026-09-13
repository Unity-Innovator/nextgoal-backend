package com.example.genz.auth.config;

import lombok.Data;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@Validated
@ConfigurationProperties(prefix = "otp")
public class OtpProperties {
    private String secret = "";
    @Min(1)
    private int maxAttempts = 5;
    @Min(1)
    private int resendCooldownSeconds = 60;
}
