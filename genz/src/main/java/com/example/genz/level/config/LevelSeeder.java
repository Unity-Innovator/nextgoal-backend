package com.example.genz.level.config;

import com.example.genz.level.entity.Level;
import com.example.genz.level.repository.LevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class LevelSeeder {

    private final LevelRepository levelRepository;

    @Bean
    public CommandLineRunner seedLevels() {
        return args -> {
            if (levelRepository.count() == 0) {
                log.info("Seeding default levels into database...");
                List<Level> defaultLevels = List.of(
                        createLevel(1, "Rookie Saver", 0),
                        createLevel(2, "Smart Saver", 100),
                        createLevel(3, "Goal Builder", 300),
                        createLevel(4, "Saving Master", 600),
                        createLevel(5, "Financial Explorer", 1000),
                        createLevel(6, "Wealth Builder", 1500),
                        createLevel(7, "Capital Champion", 2200),
                        createLevel(8, "Investment Prodigy", 3000),
                        createLevel(9, "Diamond Saver", 4000),
                        createLevel(10, "Legendary Saver", 5000)
                );
                levelRepository.saveAll(defaultLevels);
                log.info("Successfully seeded {} levels.", defaultLevels.size());
            }
        };
    }

    private Level createLevel(int levelNumber, String name, int requiredXp) {
        return Level.builder()
                .levelNumber(levelNumber)
                .name(name)
                .requiredXp(requiredXp)
                .build();
    }
}
