package com.example.genz.goal;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.enums.UserRole;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.goal.dto.CreateGoalRequest;
import com.example.genz.goal.dto.GoalResponse;
import com.example.genz.goal.dto.UpdateGoalRequest;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.enums.GoalCategory;
import com.example.genz.goal.enums.GoalStatus;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.goal.service.GoalService;
import com.example.genz.invitation.dto.CreateInvitationRequest;
import com.example.genz.invitation.dto.InvitationResponse;
import com.example.genz.invitation.enums.InvitationStatus;
import com.example.genz.invitation.service.GoalInvitationService;
import com.example.genz.transaction.enums.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_goal_visibility_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
@Transactional
class SharedGoalVisibilityIntegrationTest {

    @Autowired
    private GoalService goalService;

    @Autowired
    private GoalInvitationService goalInvitationService;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalMemberRepository goalMemberRepository;

    @Autowired
    private UserRepository userRepository;

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(User.builder()
                .phoneNumber("011111111")
                .fullName("User A")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .status("ACTIVE")
                .role(UserRole.USER)
                .build());

        userB = userRepository.save(User.builder()
                .phoneNumber("022222222")
                .fullName("User B")
                .dateOfBirth(LocalDate.of(2000, 2, 2))
                .gender(Gender.FEMALE)
                .status("ACTIVE")
                .role(UserRole.USER)
                .build());

        userC = userRepository.save(User.builder()
                .phoneNumber("033333333")
                .fullName("User C")
                .dateOfBirth(LocalDate.of(2000, 3, 3))
                .gender(Gender.OTHER)
                .status("ACTIVE")
                .role(UserRole.USER)
                .build());
    }

    private GoalResponse createSampleGoalForUserA(String name) {
        CreateGoalRequest request = new CreateGoalRequest();
        request.setGoalName(name);
        request.setTargetAmount(new BigDecimal("1000.00"));
        request.setTargetDate(LocalDate.now().plusMonths(3));
        request.setCategory(GoalCategory.TRAVEL);
        request.setCurrency(Currency.USD);
        return goalService.createGoal(request, userA.getPhoneNumber());
    }

    @Test
    @DisplayName("Scenario 1: Owner User A creates Goal A -> User A sees Goal A in goal list")
    void testOwnerSeesGoal() {
        GoalResponse created = createSampleGoalForUserA("Trip to Japan");

        List<GoalResponse> userAGoals = goalService.getMyGoals(userA.getPhoneNumber(), null);
        assertEquals(1, userAGoals.size());
        assertEquals(created.getGoalId(), userAGoals.get(0).getGoalId());
        assertEquals("Trip to Japan", userAGoals.get(0).getGoalName());
    }

    @Test
    @DisplayName("Scenario 2: Pending invitation -> User B does NOT see Goal A in goal list")
    void testPendingInvitationDoesNotSeeGoal() {
        GoalResponse created = createSampleGoalForUserA("Trip to Japan");

        // User A invites User B
        CreateInvitationRequest inviteRequest = new CreateInvitationRequest();
        inviteRequest.setInviteeId(userB.getId());
        InvitationResponse invitation = goalInvitationService.inviteUser(created.getGoalId(), inviteRequest, userA.getPhoneNumber());
        assertEquals(InvitationStatus.PENDING.name(), invitation.getStatus());

        // User B has not accepted yet -> Goal A must NOT appear in User B's goal list
        List<GoalResponse> userBGoals = goalService.getMyGoals(userB.getPhoneNumber(), null);
        assertTrue(userBGoals.isEmpty(), "User B should not see goal while invitation is pending");
    }

    @Test
    @DisplayName("Scenario 3: Non-member User C -> User C does not see Goal A")
    void testNonMemberDoesNotSeeGoal() {
        createSampleGoalForUserA("Trip to Japan");

        List<GoalResponse> userCGoals = goalService.getMyGoals(userC.getPhoneNumber(), null);
        assertTrue(userCGoals.isEmpty(), "Non-member User C must not see Goal A");
    }

    @Test
    @DisplayName("Scenario 4: Accepted member User B -> User B sees shared Goal A with exact same goalId (no duplicate goal)")
    void testAcceptedMemberSeesGoal() {
        GoalResponse created = createSampleGoalForUserA("Trip to Japan");

        // Invite and Accept
        CreateInvitationRequest inviteRequest = new CreateInvitationRequest();
        inviteRequest.setInviteeId(userB.getId());
        InvitationResponse invitation = goalInvitationService.inviteUser(created.getGoalId(), inviteRequest, userA.getPhoneNumber());

        InvitationResponse accepted = goalInvitationService.acceptInvitation(invitation.getInvitationId(), userB.getPhoneNumber());
        assertEquals(InvitationStatus.ACCEPTED.name(), accepted.getStatus());

        // Both User A and User B see the same Goal
        List<GoalResponse> userAGoals = goalService.getMyGoals(userA.getPhoneNumber(), null);
        List<GoalResponse> userBGoals = goalService.getMyGoals(userB.getPhoneNumber(), null);

        assertEquals(1, userAGoals.size(), "Owner User A should see Goal A");
        assertEquals(1, userBGoals.size(), "Accepted Member User B should see Goal A");

        assertEquals(created.getGoalId(), userAGoals.get(0).getGoalId());
        assertEquals(created.getGoalId(), userBGoals.get(0).getGoalId());

        // Only ONE Goal entity must exist in the database
        assertEquals(1, goalRepository.count(), "There must only be 1 goal entity in the database");

        // Verify active members list on Goal response includes User B
        GoalResponse bGoal = userBGoals.get(0);
        assertNotNull(bGoal.getMembers());
        assertTrue(bGoal.getMembers().stream().anyMatch(m -> m.getUserId().equals(userB.getId())));
    }

    @Test
    @DisplayName("Scenario 5: Duplicate membership prevention -> User B accepting invitation twice produces no duplicate GoalMember record")
    void testDuplicateMembershipPrevented() {
        GoalResponse created = createSampleGoalForUserA("Shared Apartment");

        CreateInvitationRequest inviteRequest = new CreateInvitationRequest();
        inviteRequest.setInviteeId(userB.getId());
        InvitationResponse invitation = goalInvitationService.inviteUser(created.getGoalId(), inviteRequest, userA.getPhoneNumber());

        // Accept first time
        goalInvitationService.acceptInvitation(invitation.getInvitationId(), userB.getPhoneNumber());

        Goal goal = goalRepository.findById(created.getGoalId()).orElseThrow();
        long memberCountAfterFirst = goalMemberRepository.findByGoal(goal).stream()
                .filter(m -> m.getUser().getId().equals(userB.getId()))
                .count();
        assertEquals(1, memberCountAfterFirst);

        // Accept second time (idempotent acceptance)
        InvitationResponse secondAccept = goalInvitationService.acceptInvitation(invitation.getInvitationId(), userB.getPhoneNumber());
        assertEquals(InvitationStatus.ACCEPTED.name(), secondAccept.getStatus());

        long memberCountAfterSecond = goalMemberRepository.findByGoal(goal).stream()
                .filter(m -> m.getUser().getId().equals(userB.getId()))
                .count();
        assertEquals(1, memberCountAfterSecond, "User B must have exactly 1 GoalMember record");
    }

    @Test
    @DisplayName("Scenario 6: Goal permissions preserved -> Member B can view but cannot update or delete Goal A")
    void testGoalPermissionsPreserved() {
        GoalResponse created = createSampleGoalForUserA("Shared Fund");

        CreateInvitationRequest inviteRequest = new CreateInvitationRequest();
        inviteRequest.setInviteeId(userB.getId());
        InvitationResponse invitation = goalInvitationService.inviteUser(created.getGoalId(), inviteRequest, userA.getPhoneNumber());
        goalInvitationService.acceptInvitation(invitation.getInvitationId(), userB.getPhoneNumber());

        // Member B can get goal details
        GoalResponse detail = goalService.getGoal(created.getGoalId(), userB.getPhoneNumber());
        assertNotNull(detail);
        assertEquals(created.getGoalId(), detail.getGoalId());

        // Member B cannot update Goal A
        UpdateGoalRequest updateRequest = new UpdateGoalRequest();
        updateRequest.setGoalName("Hacked Goal Name");
        assertThrows(AccessDeniedException.class, () ->
                goalService.updateGoal(created.getGoalId(), updateRequest, userB.getPhoneNumber())
        );

        // Member B cannot delete Goal A
        assertThrows(AccessDeniedException.class, () ->
                goalService.deleteGoal(created.getGoalId(), userB.getPhoneNumber())
        );
    }
}
