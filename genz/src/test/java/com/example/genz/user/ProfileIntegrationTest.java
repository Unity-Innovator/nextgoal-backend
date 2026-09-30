package com.example.genz.user;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.enums.UserRole;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.enums.GoalCategory;
import com.example.genz.goal.enums.GoalStatus;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.transaction.entity.GoalContribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_profile_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long",
        "jwt.access-expiration=900000",
        "jwt.refresh-expiration=604800000",
        "jwt.step-up-expiration=300000"
})
@AutoConfigureMockMvc
@Transactional
class ProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalMemberRepository goalMemberRepository;

    @Autowired
    private GoalContributionRepository goalContributionRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .fullName("Chea Teng")
                .username("cheateng")
                .phoneNumber("012345678")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .status("ACTIVE")
                .role(UserRole.USER)
                .totalXp(365)
                .coinBalance(245)
                .build();
        testUser = userRepository.save(testUser);

        // Goal 1: Active Goal
        Goal goal1 = new Goal();
        goal1.setGoalName("Emergency Fund");
        goal1.setTargetAmount(new BigDecimal("2000.00"));
        goal1.setCurrentAmount(new BigDecimal("500.00"));
        goal1.setTargetDate(LocalDate.now().plusMonths(6));
        goal1.setCategory(GoalCategory.TRAVEL);
        goal1.setStatus(GoalStatus.ACTIVE);
        goal1.setOwner(testUser);
        goal1.setCreatedAt(LocalDateTime.now());
        goal1 = goalRepository.save(goal1);

        GoalMember member1 = new GoalMember();
        member1.setGoal(goal1);
        member1.setUser(testUser);
        member1.setRole(MemberRole.OWNER);
        member1.setStatus(MemberStatus.ACTIVE);
        member1 = goalMemberRepository.save(member1);

        GoalContribution contrib1 = new GoalContribution();
        contrib1.setGoal(goal1);
        contrib1.setGoalMember(member1);
        contrib1.setAmount(new BigDecimal("500.00"));
        contrib1.setContributedAt(LocalDateTime.now());
        goalContributionRepository.save(contrib1);

        // Goal 2: Completed Goal
        Goal goal2 = new Goal();
        goal2.setGoalName("New Laptop");
        goal2.setTargetAmount(new BigDecimal("750.00"));
        goal2.setCurrentAmount(new BigDecimal("750.00"));
        goal2.setTargetDate(LocalDate.now().plusMonths(2));
        goal2.setCategory(GoalCategory.TECH);
        goal2.setStatus(GoalStatus.COMPLETED);
        goal2.setOwner(testUser);
        goal2.setCreatedAt(LocalDateTime.now().minusMonths(1));
        goal2.setCompletedAt(LocalDateTime.now());
        goal2 = goalRepository.save(goal2);

        GoalMember member2 = new GoalMember();
        member2.setGoal(goal2);
        member2.setUser(testUser);
        member2.setRole(MemberRole.OWNER);
        member2.setStatus(MemberStatus.ACTIVE);
        member2 = goalMemberRepository.save(member2);

        GoalContribution contrib2 = new GoalContribution();
        contrib2.setGoal(goal2);
        contrib2.setGoalMember(member2);
        contrib2.setAmount(new BigDecimal("750.00"));
        contrib2.setContributedAt(LocalDateTime.now());
        goalContributionRepository.save(contrib2);
    }

    @Test
    @WithMockUser(username = "012345678", roles = {"USER"})
    @DisplayName("GET /api/users/me/profile returns 200 with full profile data")
    void getMyProfile_Authenticated_ReturnsProfileResponse() throws Exception {
        mockMvc.perform(get("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile retrieved successfully"))
                .andExpect(jsonPath("$.data.user.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.data.user.fullName").value("Chea Teng"))
                .andExpect(jsonPath("$.data.user.username").value("cheateng"))
                .andExpect(jsonPath("$.data.user.phoneNumber").value("012345678"))
                .andExpect(jsonPath("$.data.user.achievementUnlockedCount").value(0))
                .andExpect(jsonPath("$.data.level.level").value(3))
                .andExpect(jsonPath("$.data.level.name").value("Goal Builder"))
                .andExpect(jsonPath("$.data.level.totalXp").value(365))
                .andExpect(jsonPath("$.data.level.xp").doesNotExist())
                .andExpect(jsonPath("$.data.level.currentLevelXp").value(300))
                .andExpect(jsonPath("$.data.level.nextLevelXp").value(600))
                .andExpect(jsonPath("$.data.level.progressPercentage").value(21.67))
                .andExpect(jsonPath("$.data.level.completedAchievements").value(0))
                .andExpect(jsonPath("$.data.level.completedAchievement").doesNotExist())
                .andExpect(jsonPath("$.data.level.streak").value(1))
                .andExpect(jsonPath("$.data.summary.totalSaved").value(1250.00))
                .andExpect(jsonPath("$.data.summary.totalGoals").value(2))
                .andExpect(jsonPath("$.data.summary.coins").value(245));
    }

    @Test
    @DisplayName("GET /api/users/me/profile unauthenticated returns 401/403")
    void getMyProfile_Unauthenticated_ReturnsForbiddenOrUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "012345678", roles = {"USER"})
    @DisplayName("GET /api/v1/profile returns 200 with profile summary data")
    void getMyProfileV1_Authenticated_ReturnsProfileV1Response() throws Exception {
        mockMvc.perform(get("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile retrieved"))
                .andExpect(jsonPath("$.data.name").value("Chea Teng"))
                .andExpect(jsonPath("$.data.username").value("cheateng"))
                .andExpect(jsonPath("$.data.phone").value("012345678"))
                .andExpect(jsonPath("$.data.birthday").value("2000-01-01"))
                .andExpect(jsonPath("$.data.gender").value("MALE"))
                .andExpect(jsonPath("$.data.level").value(1))
                .andExpect(jsonPath("$.data.goalsCount").value(2))
                .andExpect(jsonPath("$.data.coinsCount").value(245));
    }

    @Test
    @DisplayName("GET /api/v1/profile unauthenticated returns 401/403")
    void getMyProfileV1_Unauthenticated_ReturnsForbiddenOrUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "012345678", roles = {"USER"})
    @DisplayName("PUT /api/users/me/profile updates fullName, username, phone, avatarUrl and returns new tokens when phone changes")
    void updateMyProfile_Put_UpdatesProfileAndReturnsTokensOnPhoneChange() throws Exception {
        String requestBody = """
                {
                    "fullName": "Pon Kouyseng",
                    "username": "@ponkouyseng",
                    "phoneNumber": "0714232439",
                    "avatarUrl": "https://example.com/avatar.png"
                }
                """;

        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.data.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.data.fullName").value("Pon Kouyseng"))
                .andExpect(jsonPath("$.data.username").value("ponkouyseng"))
                .andExpect(jsonPath("$.data.phoneNumber").value("0714232439"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @WithMockUser(username = "012345678", roles = {"USER"})
    @DisplayName("PATCH /api/v1/profile updates fields without re-issuing tokens when phone number is unchanged")
    void updateMyProfile_Patch_NoPhoneChangeDoesNotReissueTokens() throws Exception {
        String requestBody = """
                {
                    "fullName": "Pon Kouyseng",
                    "username": "ponkouyseng"
                }
                """;

        mockMvc.perform(patch("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Pon Kouyseng"))
                .andExpect(jsonPath("$.data.username").value("ponkouyseng"))
                .andExpect(jsonPath("$.data.phoneNumber").value("012345678"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());
    }

    @Test
    @WithMockUser(username = "012345678", roles = {"USER"})
    @DisplayName("PUT /api/users/me/profile returns 400 when username or phone number already belongs to another user")
    void updateMyProfile_DuplicatePhoneOrUsername_ReturnsBadRequest() throws Exception {
        User otherUser = User.builder()
                .fullName("Other User")
                .username("existinguser")
                .phoneNumber("099888777")
                .dateOfBirth(LocalDate.of(1999, 5, 5))
                .gender(Gender.FEMALE)
                .status("ACTIVE")
                .role(UserRole.USER)
                .build();
        userRepository.save(otherUser);

        String duplicateUsernameRequest = """
                {
                    "username": "@ExistingUser"
                }
                """;

        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateUsernameRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists"));

        String duplicatePhoneRequest = """
                {
                    "phoneNumber": "099888777"
                }
                """;

        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicatePhoneRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Phone number already exists"));
    }
}
