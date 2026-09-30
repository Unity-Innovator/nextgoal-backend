package com.example.genz.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @JsonAlias({"name"})
    @Size(max = 255, message = "Full name must be at most 255 characters")
    private String fullName;

    @Size(max = 50, message = "Username must be at most 50 characters")
    private String username;

    @JsonAlias({"phone"})
    @Size(max = 30, message = "Phone number must be at most 30 characters")
    private String phoneNumber;

    @JsonAlias({"photoUrl", "avatar"})
    @Size(max = 2048, message = "Avatar URL must be at most 2048 characters")
    private String avatarUrl;
}
