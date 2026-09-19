package com.example.genz.level.service;

import com.example.genz.user.dto.ProfileLevelResponse;

public interface LevelService {

    ProfileLevelResponse calculateLevelProgress(Integer userXp);
}
