package com.example.genz.level;

import com.example.genz.level.entity.Level;
import com.example.genz.level.repository.LevelRepository;
import com.example.genz.level.service.LevelServiceImpl;
import com.example.genz.user.dto.ProfileLevelResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LevelServiceTest {

    @Mock
    private LevelRepository levelRepository;

    @InjectMocks
    private LevelServiceImpl levelService;

    private List<Level> mockLevels;

    @BeforeEach
    void setUp() {
        mockLevels = List.of(
                Level.builder().id(UUID.randomUUID()).levelNumber(1).name("Rookie Saver").requiredXp(0).build(),
                Level.builder().id(UUID.randomUUID()).levelNumber(2).name("Smart Saver").requiredXp(100).build(),
                Level.builder().id(UUID.randomUUID()).levelNumber(3).name("Goal Builder").requiredXp(300).build(),
                Level.builder().id(UUID.randomUUID()).levelNumber(4).name("Saving Master").requiredXp(600).build(),
                Level.builder().id(UUID.randomUUID()).levelNumber(5).name("Financial Explorer").requiredXp(1000).build(),
                Level.builder().id(UUID.randomUUID()).levelNumber(6).name("Wealth Builder").requiredXp(1500).build(),
                Level.builder().id(UUID.randomUUID()).levelNumber(7).name("Capital Champion").requiredXp(2200).build(),
                Level.builder().id(UUID.randomUUID()).levelNumber(8).name("Investment Prodigy").requiredXp(3000).build(),
                Level.builder().id(UUID.randomUUID()).levelNumber(9).name("Diamond Saver").requiredXp(4000).build(),
                Level.builder().id(UUID.randomUUID()).levelNumber(10).name("Legendary Saver").requiredXp(5000).build()
        );
    }

    @Test
    @DisplayName("Case 1: totalXp = 0 -> Level 1, progress 0.0%")
    void testTotalXpZero() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(0);

        assertNotNull(response);
        assertEquals(1, response.getLevel());
        assertEquals("Rookie Saver", response.getName());
        assertEquals(0, response.getTotalXp());
        assertEquals(0, response.getCurrentLevelXp());
        assertEquals(100, response.getNextLevelXp());
        assertEquals(0.0, response.getProgressPercentage());
    }

    @Test
    @DisplayName("Case 2: totalXp = 65 -> Level 1, progress 65.0%")
    void testTotalXp65() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(65);

        assertNotNull(response);
        assertEquals(1, response.getLevel());
        assertEquals("Rookie Saver", response.getName());
        assertEquals(65, response.getTotalXp());
        assertEquals(0, response.getCurrentLevelXp());
        assertEquals(100, response.getNextLevelXp());
        assertEquals(65.0, response.getProgressPercentage());
    }

    @Test
    @DisplayName("Case 3: totalXp = 100 -> Level 2, progress 0.0%")
    void testTotalXp100() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(100);

        assertNotNull(response);
        assertEquals(2, response.getLevel());
        assertEquals("Smart Saver", response.getName());
        assertEquals(100, response.getTotalXp());
        assertEquals(100, response.getCurrentLevelXp());
        assertEquals(300, response.getNextLevelXp());
        assertEquals(0.0, response.getProgressPercentage());
    }

    @Test
    @DisplayName("Case 4: totalXp = 299 -> Level 2, progress 99.5%")
    void testTotalXp299() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(299);

        assertNotNull(response);
        assertEquals(2, response.getLevel());
        assertEquals("Smart Saver", response.getName());
        assertEquals(299, response.getTotalXp());
        assertEquals(100, response.getCurrentLevelXp());
        assertEquals(300, response.getNextLevelXp());
        assertEquals(99.5, response.getProgressPercentage());
    }

    @Test
    @DisplayName("Case 5: totalXp = 300 -> Level 3, progress 0.0%")
    void testTotalXp300() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(300);

        assertNotNull(response);
        assertEquals(3, response.getLevel());
        assertEquals("Goal Builder", response.getName());
        assertEquals(300, response.getTotalXp());
        assertEquals(300, response.getCurrentLevelXp());
        assertEquals(600, response.getNextLevelXp());
        assertEquals(0.0, response.getProgressPercentage());
    }

    @Test
    @DisplayName("Case 6: totalXp = 365 -> Level 3, progress 21.67%")
    void testTotalXp365() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(365);

        assertNotNull(response);
        assertEquals(3, response.getLevel());
        assertEquals("Goal Builder", response.getName());
        assertEquals(365, response.getTotalXp());
        assertEquals(300, response.getCurrentLevelXp());
        assertEquals(600, response.getNextLevelXp());
        assertEquals(21.67, response.getProgressPercentage());
    }

    @Test
    @DisplayName("Case 7: totalXp = 599 -> Level 3, progress 99.67%")
    void testTotalXp599() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(599);

        assertNotNull(response);
        assertEquals(3, response.getLevel());
        assertEquals("Goal Builder", response.getName());
        assertEquals(599, response.getTotalXp());
        assertEquals(300, response.getCurrentLevelXp());
        assertEquals(600, response.getNextLevelXp());
        assertEquals(99.67, response.getProgressPercentage());
    }

    @Test
    @DisplayName("Case 8: totalXp = 600 -> Level 4, progress 0.0%")
    void testTotalXp600() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(600);

        assertNotNull(response);
        assertEquals(4, response.getLevel());
        assertEquals("Saving Master", response.getName());
        assertEquals(600, response.getTotalXp());
        assertEquals(600, response.getCurrentLevelXp());
        assertEquals(1000, response.getNextLevelXp());
        assertEquals(0.0, response.getProgressPercentage());
    }

    @Test
    @DisplayName("Case 9: totalXp = 5000 (Max Level boundary) -> Level 10, progress 100.0%")
    void testTotalXpAtMaxLevel() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(5000);

        assertNotNull(response);
        assertEquals(10, response.getLevel());
        assertEquals("Legendary Saver", response.getName());
        assertEquals(5000, response.getTotalXp());
        assertEquals(5000, response.getCurrentLevelXp());
        assertNull(response.getNextLevelXp());
        assertEquals(100.0, response.getProgressPercentage());
    }

    @Test
    @DisplayName("Case 9b: totalXp = 8500 (Above Max Level) -> Level 10, progress 100.0%")
    void testTotalXpAboveMaxLevel() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(8500);

        assertNotNull(response);
        assertEquals(10, response.getLevel());
        assertEquals("Legendary Saver", response.getName());
        assertEquals(8500, response.getTotalXp());
        assertEquals(5000, response.getCurrentLevelXp());
        assertNull(response.getNextLevelXp());
        assertEquals(100.0, response.getProgressPercentage());
    }

    @Test
    @DisplayName("Negative XP clamped to Level 1")
    void testXpBelowFirstThreshold() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(-50);

        assertNotNull(response);
        assertEquals(1, response.getLevel());
        assertEquals("Rookie Saver", response.getName());
        assertEquals(-50, response.getTotalXp());
        assertEquals(0, response.getCurrentLevelXp());
        assertEquals(100, response.getNextLevelXp());
        assertEquals(0.0, response.getProgressPercentage());
    }

    @Test
    @DisplayName("Null XP defaults to 0 XP and Level 1")
    void testNullXpHandling() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(null);

        assertNotNull(response);
        assertEquals(1, response.getLevel());
        assertEquals("Rookie Saver", response.getName());
        assertEquals(0, response.getTotalXp());
        assertEquals(0.0, response.getProgressPercentage());
    }
}
