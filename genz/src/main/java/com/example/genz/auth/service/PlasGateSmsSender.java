package com.example.genz.auth.service;

import com.example.genz.auth.dto.PlasGateSmsRequest;
import com.example.genz.auth.exception.OtpException;
import com.example.genz.config.PlasGateProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PlasGateSmsSender {
    private final RestClient client;
    private final PlasGateProperties properties;

    public PlasGateSmsSender(@Qualifier("plasGateRestClient") RestClient client, PlasGateProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public void sendOtp(String phone, String otp) {
        if (!StringUtils.hasText(properties.baseUrl())
                || !StringUtils.hasText(properties.privateKey())
                || !StringUtils.hasText(properties.secretKey())
                || !StringUtils.hasText(properties.sender())) {
            throw new OtpException("SMS delivery is not configured");
        }
        String destination = phone.startsWith("+") ? phone.substring(1) : phone;
        if (destination.startsWith("0")) destination = "855" + destination.substring(1);
        try {
            client.post()
                    .uri(builder -> builder.path("/rest/send")
                            .queryParam("private_key", "{privateKey}").build(properties.privateKey()))
                    .header("X-Secret", properties.secretKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PlasGateSmsRequest(properties.sender(), destination,
                            "Your NextGoal OTP is #ma#" + otp + "#ma#. It expires in 5 minutes."))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException | IllegalArgumentException ex) {
            // Provider exceptions can contain credentials and message contents. Do not expose them.
            throw new OtpException("Unable to send OTP. Please try again later.");
        }
    }
}
