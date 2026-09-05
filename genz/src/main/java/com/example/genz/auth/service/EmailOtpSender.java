package com.example.genz.auth.service;

import com.example.genz.auth.enums.OtpPurpose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailOtpSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailOtpSender(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendOtp(String recipient, String otp, OtpPurpose purpose, int expirationMinutes) {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("MAIL_USERNAME must be configured before sending OTP email");
        }

        String action = purpose == OtpPurpose.REGISTRATION
                ? "complete your registration"
                : "reset your password";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("Your NextGoal verification code");
        message.setText("""
                Your NextGoal verification code is: %s

                Use this code to %s. It expires in %d minutes.
                Never share this code with anyone.
                """.formatted(otp, action, expirationMinutes));

        mailSender.send(message);
    }
}
