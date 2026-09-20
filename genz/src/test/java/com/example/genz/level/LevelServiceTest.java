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
    @DisplayName("XP below first threshold should clamp progress to 0 and remain at Level 1")
    void testXpBelowFirstThreshold() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(-50);

        assertNotNull(response);
        assertEquals(1, response.getLevel());
        assertEquals("Rookie Saver", response.getName());
        assertEquals(-50, response.getXp());
        assertEquals(0, response.getCurrentLevelXp());
        assertEquals(100, response.getNextLevelXp());
        assertEquals(0.0, response.getProgressPercentage());
    }

    @Test
    @DisplayName("XP exactly at Level 1 threshold (0 XP)")
    void testXpAtLevelOneThreshold() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(0);

        assertNotNull(response);
        assertEquals(1, response.getLevel());
        assertEquals("Rookie Saver", response.getName());
        assertEquals(0, response.getXp());
        assertEquals(0, response.getCurrentLevelXp());
        assertEquals(100, response.getNextLevelXp());
        assertEquals(0.0, response.getProgressPercentage());
    }

    @Test
    @DisplayName("XP exactly at Level 2 threshold (100 XP)")
    void testXpAtLevelTwoThreshold() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(100);

        assertNotNull(response);
        assertEquals(2, response.getLevel());
        assertEquals("Smart Saver", response.getName());
        assertEquals(100, response.getXp());
        assertEquals(100, response.getCurrentLevelXp());
        assertEquals(300, response.getNextLevelXp());
        assertEquals(0.0, response.getProgressPercentage());
    }

    @Test
    @DisplayName("XP between levels (350 XP should be Level 3 with 16.67% progress)")
    void testXpBetweenLevels() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(350);

        assertNotNull(response);
        assertEquals(3, response.getLevel());
        assertEquals("Goal Builder", response.getName());
        assertEquals(350, response.getXp());
        assertEquals(300, response.getCurrentLevelXp());
        assertEquals(600, response.getNextLevelXp());
        assertEquals(16.67, response.getProgressPercentage());
    }

    @Test
    @DisplayName("XP exactly at maximum level threshold (5000 XP)")
    void testXpAtMaxLevel() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(5000);

        assertNotNull(response);
        assertEquals(10, response.getLevel());
        assertEquals("Legendary Saver", response.getName());
        assertEquals(5000, response.getXp());
        assertEquals(5000, response.getCurrentLevelXp());
        assertNull(response.getNextLevelXp());
        assertEquals(100.0, response.getProgressPercentage());
    }

    @Test
    @DisplayName("XP well above maximum level threshold (8500 XP)")
    void testXpAboveMaxLevel() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(8500);

        assertNotNull(response);
        assertEquals(10, response.getLevel());
        assertEquals("Legendary Saver", response.getName());
        assertEquals(8500, response.getXp());
        assertEquals(5000, response.getCurrentLevelXp());
        assertNull(response.getNextLevelXp());
        assertEquals(100.0, response.getProgressPercentage());
    }

    @Test
    @DisplayName("Null XP should default to 0 XP and Level 1")
    void testNullXpHandling() {
        when(levelRepository.findAllByOrderByLevelNumberAsc()).thenReturn(mockLevels);

        ProfileLevelResponse response = levelService.calculateLevelProgress(null);

        assertNotNull(response);
        assertEquals(1, response.getLevel());
        assertEquals("Rookie Saver", response.getName());
        assertEquals(0, response.getXp());
        assertEquals(0.0, response.getProgressPercentage());
    }
}
