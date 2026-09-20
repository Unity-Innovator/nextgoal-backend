package com.example.genz.user.service;

import com.example.genz.auth.entity.User;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.level.service.LevelService;
import com.example.genz.user.dto.ProfileLevelResponse;
import com.example.genz.user.dto.ProfileResponse;
import com.example.genz.user.mapper.ProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final LevelService levelService;
    private final GoalRepository goalRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final ProfileMapper profileMapper;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getUserProfile(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        int xp = user.getTotalXp() != null ? user.getTotalXp() : 0;
        ProfileLevelResponse levelResponse = levelService.calculateLevelProgress(xp);

        BigDecimal rawTotalSaved = goalContributionRepository.sumAmountByUserId(user.getId());
        BigDecimal totalSaved = (rawTotalSaved != null ? rawTotalSaved : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        long totalGoals = goalRepository.countByOwner(user);
        int coins = user.getCoinBalance() != null ? user.getCoinBalance() : 0;

        return profileMapper.toProfileResponse(user, levelResponse, totalSaved, totalGoals, coins);
    }
}
