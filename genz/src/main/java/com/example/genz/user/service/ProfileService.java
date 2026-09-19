package com.example.genz.user.service;

import com.example.genz.auth.entity.User;
import com.example.genz.user.dto.ProfileResponse;

public interface ProfileService {

    ProfileResponse getUserProfile(User user);
}
