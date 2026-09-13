package com.example.genz.auth.service;

import com.example.genz.auth.exception.OtpException;
import com.example.genz.config.PlasGateProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PlasGateSmsSenderTest {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://cloudapi.plasgate.com");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final PlasGateSmsSender sender = new PlasGateSmsSender(builder.build(),
            new PlasGateProperties("https://cloudapi.plasgate.com", "private-test", "secret-test", "NextGoal"));

    @Test
    void sendsAuthenticatedRequestWithMaskedCodeAndCambodianDestination() {
        server.expect(requestTo("https://cloudapi.plasgate.com/rest/send?private_key=private-test"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Secret", "secret-test"))
                .andExpect(jsonPath("$.sender").value("NextGoal"))
                .andExpect(jsonPath("$.to").value("85512345678"))
                .andExpect(jsonPath("$.content").value("Your NextGoal OTP is #ma#123456#ma#. It expires in 5 minutes."))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        sender.sendOtp("012345678", "123456");
        server.verify();
    }

    @Test
    void providerFailureDoesNotExposeCredentials() {
        server.expect(requestTo("https://cloudapi.plasgate.com/rest/send?private_key=private-test"))
                .andRespond(withServerError());
        OtpException ex = assertThrows(OtpException.class, () -> sender.sendOtp("+85512345678", "123456"));
        assertEquals("Unable to send OTP. Please try again later.", ex.getMessage());
        assertNull(ex.getCause());
        server.verify();
    }

    @Test
    void missingCredentialsFailWithoutSending() {
        var unconfigured = new PlasGateSmsSender(builder.build(),
                new PlasGateProperties("https://cloudapi.plasgate.com", "", "", ""));
        assertThrows(OtpException.class, () -> unconfigured.sendOtp("012345678", "123456"));
        server.verify();
    }
}
