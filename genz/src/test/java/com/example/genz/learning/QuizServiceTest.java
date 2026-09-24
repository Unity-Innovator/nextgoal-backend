package com.example.genz.learning;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.learning.dto.QuizAnswerItem;
import com.example.genz.learning.dto.QuizResultResponse;
import com.example.genz.learning.dto.QuizStartResponse;
import com.example.genz.learning.dto.QuizSubmitRequest;
import com.example.genz.learning.entity.Lesson;
import com.example.genz.learning.entity.LessonQuestion;
import com.example.genz.learning.entity.QuizAttempt;
import com.example.genz.learning.enums.AnswerChoice;
import com.example.genz.learning.enums.LessonCategory;
import com.example.genz.learning.enums.LessonDifficulty;
import com.example.genz.learning.mapper.QuizMapper;
import com.example.genz.learning.repository.LessonQuestionRepository;
import com.example.genz.learning.repository.QuizAttemptRepository;
import com.example.genz.learning.repository.UserAnswerRepository;
import com.example.genz.learning.service.QuizServiceImpl;
import com.example.genz.reward.entity.UserReward;
import com.example.genz.reward.repository.UserRewardRepository;
import com.example.genz.reward.repository.XpTransactionRepository;
import com.example.genz.reward.service.AchievementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private LessonQuestionRepository lessonQuestionRepository;

    @Mock
    private UserAnswerRepository userAnswerRepository;

    @Mock
    private UserRewardRepository userRewardRepository;

    @Mock
    private XpTransactionRepository xpTransactionRepository;

    @Mock
    private AchievementService achievementService;

    @Spy
    private QuizMapper quizMapper;

    @InjectMocks
    private QuizServiceImpl quizService;

    private User testUser;
    private User otherUser;
    private List<LessonQuestion> testQuestions;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("John Doe")
                .phoneNumber("0123456789")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .status("ACTIVE")
                .energy(100)
                .totalXp(0)
                .energyResetDate(LocalDate.now())
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Jane Doe")
                .phoneNumber("0987654321")
                .dateOfBirth(LocalDate.of(2001, 2, 2))
                .gender(Gender.FEMALE)
                .status("ACTIVE")
                .energy(100)
                .totalXp(0)
                .energyResetDate(LocalDate.now())
                .build();

        Lesson testLesson = new Lesson();
        testLesson.setId(UUID.randomUUID());
        testLesson.setTitle("Financial Basics");
        testLesson.setCategory(LessonCategory.SAVING);
        testLesson.setDifficulty(LessonDifficulty.EASY);
        testLesson.setIsActive(true);

        testQuestions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            LessonQuestion q = new LessonQuestion();
            q.setId(UUID.randomUUID());
            q.setLesson(testLesson);
            q.setQuestion("Question " + i);
            q.setOptionA("Option A");
            q.setOptionB("Option B");
            q.setOptionC("Option C");
            q.setOptionD("Option D");
            q.setCorrectAnswer("A");
            q.setOrderIndex(i);
            testQuestions.add(q);
        }
    }

    @Test
    @DisplayName("Should successfully start quiz when energy is >= 50")
    void startQuiz_Success() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(lessonQuestionRepository.findActiveQuestionsUnansweredByUser(testUser)).thenReturn(testQuestions);
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> {
            QuizAttempt attempt = invocation.getArgument(0);
            attempt.setId(UUID.randomUUID());
            return attempt;
        });

        QuizStartResponse response = quizService.startQuiz(testUser);

        assertNotNull(response);
        assertNotNull(response.getQuizAttemptId());
        assertEquals(5, response.getTotalQuestions());
        assertEquals(5, response.getQuestions().size());
        assertEquals(100, response.getRemainingEnergy());

        // Verify question response contains options but NO correct answer field
        assertEquals("Option A", response.getQuestions().get(0).getOptionA());
    }

    @Test
    @DisplayName("Should reject starting quiz when energy is < 50")
    void startQuiz_InsufficientEnergy_ThrowsException() {
        testUser.setEnergy(40);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> quizService.startQuiz(testUser));
        assertTrue(ex.getMessage().contains("Insufficient energy"));
    }

    @Test
    @DisplayName("Should reset energy to 100 on new day before energy check")
    void startQuiz_DailyReset() {
        testUser.setEnergy(0);
        testUser.setEnergyResetDate(LocalDate.now().minusDays(1));

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonQuestionRepository.findActiveQuestionsUnansweredByUser(testUser)).thenReturn(testQuestions);
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> {
            QuizAttempt attempt = invocation.getArgument(0);
            attempt.setId(UUID.randomUUID());
            return attempt;
        });

        QuizStartResponse response = quizService.startQuiz(testUser);

        assertEquals(100, testUser.getEnergy());
        assertEquals(LocalDate.now(), testUser.getEnergyResetDate());
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should submit quiz and calculate XP for 5/5 correct (70 XP)")
    void submitQuiz_FiveOutofFive_CorrectXpCalculation() {
        UUID attemptId = UUID.randomUUID();
        QuizAttempt attempt = QuizAttempt.builder()
                .id(attemptId)
                .user(testUser)
                .totalQuestions(5)
                .isCompleted(false)
                .questions(testQuestions)
                .build();

        List<QuizAnswerItem> answers = new ArrayList<>();
        for (LessonQuestion q : testQuestions) {
            answers.add(new QuizAnswerItem(q.getId(), AnswerChoice.A));
        }
        QuizSubmitRequest request = new QuizSubmitRequest(answers);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(achievementService.getOrCreateUserReward(testUser)).thenReturn(UserReward.builder().totalXp(0).level(1).build());
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuizResultResponse response = quizService.submitQuiz(attemptId, request, testUser);

        assertNotNull(response);
        assertEquals(5, response.getCorrectAnswers());
        assertEquals(100, response.getScorePercentage());
        assertEquals(50, response.getBaseXp());
        assertEquals(20, response.getBonusXp());
        assertEquals(70, response.getTotalXpEarned());
        assertEquals(50, response.getRemainingEnergy());
        assertEquals(50, testUser.getEnergy());
        assertEquals(70, testUser.getTotalXp());
        assertTrue(attempt.getIsCompleted());
    }

    @Test
    @DisplayName("Should submit quiz and calculate XP for 3/5 correct (35 XP)")
    void submitQuiz_ThreeOutofFive_CorrectXpCalculation() {
        UUID attemptId = UUID.randomUUID();
        QuizAttempt attempt = QuizAttempt.builder()
                .id(attemptId)
                .user(testUser)
                .totalQuestions(5)
                .isCompleted(false)
                .questions(testQuestions)
                .build();

        List<QuizAnswerItem> answers = new ArrayList<>();
        // 3 correct (A), 2 wrong (B)
        for (int i = 0; i < 5; i++) {
            AnswerChoice choice = i < 3 ? AnswerChoice.A : AnswerChoice.B;
            answers.add(new QuizAnswerItem(testQuestions.get(i).getId(), choice));
        }
        QuizSubmitRequest request = new QuizSubmitRequest(answers);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(achievementService.getOrCreateUserReward(testUser)).thenReturn(UserReward.builder().totalXp(0).level(1).build());
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuizResultResponse response = quizService.submitQuiz(attemptId, request, testUser);

        assertEquals(3, response.getCorrectAnswers());
        assertEquals(60, response.getScorePercentage());
        assertEquals(30, response.getBaseXp());
        assertEquals(5, response.getBonusXp());
        assertEquals(35, response.getTotalXpEarned());
        assertEquals(50, response.getRemainingEnergy());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when submitting another user's quiz attempt")
    void submitQuiz_UnauthorizedUser_ThrowsException() {
        UUID attemptId = UUID.randomUUID();
        QuizAttempt attempt = QuizAttempt.builder()
                .id(attemptId)
                .user(otherUser)
                .totalQuestions(5)
                .isCompleted(false)
                .questions(testQuestions)
                .build();

        QuizSubmitRequest request = new QuizSubmitRequest(List.of());

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        assertThrows(AccessDeniedException.class, () -> quizService.submitQuiz(attemptId, request, testUser));
    }

    @Test
    @DisplayName("Should throw BadRequestException when submitting an already completed quiz attempt")
    void submitQuiz_AlreadyCompleted_ThrowsException() {
        UUID attemptId = UUID.randomUUID();
        QuizAttempt attempt = QuizAttempt.builder()
                .id(attemptId)
                .user(testUser)
                .totalQuestions(5)
                .isCompleted(true)
                .questions(testQuestions)
                .build();

        QuizSubmitRequest request = new QuizSubmitRequest(List.of());

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> quizService.submitQuiz(attemptId, request, testUser));
        assertTrue(ex.getMessage().contains("already been completed"));
    }

    @Test
    @DisplayName("Should throw BadRequestException when submitting answer for question not in attempt")
    void submitQuiz_InvalidQuestionId_ThrowsException() {
        UUID attemptId = UUID.randomUUID();
        QuizAttempt attempt = QuizAttempt.builder()
                .id(attemptId)
                .user(testUser)
                .totalQuestions(5)
                .isCompleted(false)
                .questions(testQuestions)
                .build();

        List<QuizAnswerItem> answers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            answers.add(new QuizAnswerItem(testQuestions.get(i).getId(), AnswerChoice.A));
        }
        // Add random invalid question ID
        answers.add(new QuizAnswerItem(UUID.randomUUID(), AnswerChoice.A));
        QuizSubmitRequest request = new QuizSubmitRequest(answers);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> quizService.submitQuiz(attemptId, request, testUser));
        assertTrue(ex.getMessage().contains("does not belong to this quiz attempt"));
    }
}
