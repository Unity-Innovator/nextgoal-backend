package com.example.genz.user.controller;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.api.ApiResponse;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.user.dto.ProfileResponse;
import com.example.genz.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;

    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(Authentication authentication) {
        User user = getUser(authentication);
        ProfileResponse response = profileService.getUserProfile(user);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", response));
    }

    private User getUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNoteFoundException("User not authenticated");
        }
        if (authentication.getPrincipal() instanceof User u) {
            return userRepository.findById(u.getId())
                    .orElseThrow(() -> new ResourceNoteFoundException("User not found with ID: " + u.getId()));
        }
        return userRepository.findByPhoneNumber(authentication.getName())
                .orElseThrow(() -> new ResourceNoteFoundException("User not found with phone number: " + authentication.getName()));
    }
}
