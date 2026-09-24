package com.example.genz.level.service;

import com.example.genz.level.entity.Level;
import com.example.genz.level.repository.LevelRepository;
import com.example.genz.user.dto.ProfileLevelResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LevelServiceImpl implements LevelService {

    private final LevelRepository levelRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfileLevelResponse calculateLevelProgress(Integer totalXp) {
        int xp = totalXp != null ? totalXp : 0;
        List<Level> levels = levelRepository.findAllByOrderByLevelNumberAsc();
        if (levels == null || levels.isEmpty()) {
            throw new IllegalStateException("No levels configured in the system");
        }

        Level currentLevel = levels.get(0);
        Level nextLevel = levels.size() > 1 ? levels.get(1) : null;

        for (int i = 0; i < levels.size(); i++) {
            if (xp >= levels.get(i).getRequiredXp()) {
                currentLevel = levels.get(i);
                nextLevel = (i + 1 < levels.size()) ? levels.get(i + 1) : null;
            } else {
                break;
            }
        }

        int currentLevelXp = currentLevel.getRequiredXp();
        Integer nextLevelXp = nextLevel != null ? nextLevel.getRequiredXp() : null;
        double progressPercentage;

        if (nextLevel == null) {
            // Maximum level reached
            progressPercentage = 100.0;
        } else {
            int range = nextLevelXp - currentLevelXp;
            if (range <= 0) {
                progressPercentage = 100.0;
            } else {
                double raw = ((double) (xp - currentLevelXp) / range) * 100.0;
                double clamped = Math.max(0.0, Math.min(100.0, raw));
                progressPercentage = BigDecimal.valueOf(clamped)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();
            }
        }

        return ProfileLevelResponse.builder()
                .level(currentLevel.getLevelNumber())
                .name(currentLevel.getName())
                .totalXp(xp)
                .currentLevelXp(currentLevelXp)
                .nextLevelXp(nextLevelXp)
                .progressPercentage(progressPercentage)
                .build();
    }
}
