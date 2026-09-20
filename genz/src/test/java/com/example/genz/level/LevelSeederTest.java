package com.example.genz.level;

import com.example.genz.level.config.LevelSeeder;
import com.example.genz.level.entity.Level;
import com.example.genz.level.repository.LevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_level_seeder_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
@Transactional
class LevelSeederTest {

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private LevelSeeder levelSeeder;

    @BeforeEach
    void setUp() {
        levelRepository.deleteAll();
    }

    @Test
    @DisplayName("Should seed exactly 10 levels with correct names and XP thresholds")
    void seedLevels_WhenEmpty_SeedsSuccessfully() throws Exception {
        assertEquals(0, levelRepository.count());

        CommandLineRunner runner = levelSeeder.seedLevels();
        runner.run();

        assertEquals(10, levelRepository.count());

        List<Level> levels = levelRepository.findAllByOrderByLevelNumberAsc();
        assertEquals("Rookie Saver", levels.get(0).getName());
        assertEquals(0, levels.get(0).getRequiredXp());

        assertEquals("Smart Saver", levels.get(1).getName());
        assertEquals(100, levels.get(1).getRequiredXp());

        assertEquals("Goal Builder", levels.get(2).getName());
        assertEquals(300, levels.get(2).getRequiredXp());

        assertEquals("Saving Master", levels.get(3).getName());
        assertEquals(600, levels.get(3).getRequiredXp());

        assertEquals("Financial Explorer", levels.get(4).getName());
        assertEquals(1000, levels.get(4).getRequiredXp());

        assertEquals("Legendary Saver", levels.get(9).getName());
        assertEquals(5000, levels.get(9).getRequiredXp());
    }

    @Test
    @DisplayName("Should not duplicate levels if already seeded")
    void seedLevels_WhenNotEmpty_DoesNotDuplicate() throws Exception {
        CommandLineRunner runner = levelSeeder.seedLevels();
        runner.run();
        assertEquals(10, levelRepository.count());

        // Run again
        runner.run();
        assertEquals(10, levelRepository.count());
    }
}
