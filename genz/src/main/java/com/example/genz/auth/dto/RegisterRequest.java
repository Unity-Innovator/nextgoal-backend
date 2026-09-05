package com.example.genz.auth.dto;

import java.time.LocalDate;

import com.example.genz.auth.enums.Gender;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest {
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be at most 100 characters")
    private String firstName;

    @NotBlank(message = "Last name if required")
    @Size(max = 100, message = "Last name must be at most 100 characters")
    private String lastName;

    @NotBlank(message = "Phone number is required")
    @Size(max = 30, message = "Phone number must be at most 30 characters")
    private String phoneNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    @Size(max = 254)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password must be at most 72 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "Password must be at least 8 characters and include uppercase, lowercase, and a number"
    )
    private String password;

    @NotBlank(message = "Confirm password is required")
    @Size(max = 72, message = "Confirm password must be at most 72 characters")
    private String confirmPassword;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^\\d{6}$", message = "PIN must be exactly 6 digits")
    private String pin;

    @NotBlank(message = "Verification token is required")
    @com.fasterxml.jackson.annotation.JsonAlias({"token", "resetToken"})
    private String verificationToken;
}
