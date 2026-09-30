package com.example.genz.user.service;

import com.example.genz.auth.entity.User;
import com.example.genz.user.dto.ProfileResponse;
import com.example.genz.user.dto.UpdateProfileRequest;
import com.example.genz.user.dto.UpdateProfileResponse;

public interface ProfileService {

    ProfileResponse getUserProfile(User user);

    UpdateProfileResponse updateProfile(User user, UpdateProfileRequest request);
}
