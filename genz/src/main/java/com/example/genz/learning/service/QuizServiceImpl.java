package com.example.genz.learning.service;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.learning.dto.*;
import com.example.genz.learning.entity.LessonQuestion;
import com.example.genz.learning.entity.QuizAttempt;
import com.example.genz.learning.entity.UserAnswer;
import com.example.genz.learning.mapper.QuizMapper;
import com.example.genz.learning.repository.LessonQuestionRepository;
import com.example.genz.learning.repository.QuizAttemptRepository;
import com.example.genz.learning.repository.UserAnswerRepository;
import com.example.genz.reward.entity.UserReward;
import com.example.genz.reward.entity.XpTransaction;
import com.example.genz.reward.enums.ActivityType;
import com.example.genz.reward.repository.UserRewardRepository;
import com.example.genz.reward.repository.XpTransactionRepository;
import com.example.genz.reward.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private static final int REQUIRED_ENERGY = 50;
    private static final int MAX_ENERGY = 100;
    private static final int QUIZ_QUESTION_COUNT = 5;

    private final UserRepository userRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final LessonQuestionRepository lessonQuestionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserRewardRepository userRewardRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final AchievementService achievementService;
    private final QuizMapper quizMapper;

    @Override
    @Transactional
    public QuizStartResponse startQuiz(User user) {
        User managedUser = getUserOrThrow(user);

        // 1. Perform daily energy reset if date changed
        checkAndResetDailyEnergy(managedUser);

        // 2. Check user's energy
        if (managedUser.getEnergy() < REQUIRED_ENERGY) {
            throw new BadRequestException("Insufficient energy to start quiz. Minimum " + REQUIRED_ENERGY + " energy required.");
        }

        // 3. Select exactly 5 questions (prefer questions not recently answered)
        List<LessonQuestion> selectedQuestions = selectQuizQuestions(managedUser);

        // 4. Create QuizAttempt
        QuizAttempt attempt = QuizAttempt.builder()
                .user(managedUser)
                .totalQuestions(QUIZ_QUESTION_COUNT)
                .isCompleted(false)
                .questions(selectedQuestions)
                .build();

        attempt = quizAttemptRepository.save(attempt);

        return quizMapper.toQuizStartResponse(attempt, selectedQuestions, managedUser.getEnergy());
    }

    @Override
    @Transactional
    public QuizResultResponse submitQuiz(UUID quizAttemptId, QuizSubmitRequest request, User user) {
        User managedUser = getUserOrThrow(user);

        // 1. Perform daily energy reset if needed
        checkAndResetDailyEnergy(managedUser);

        // 2. Verify attempt exists and belongs to authenticated user
        QuizAttempt attempt = quizAttemptRepository.findById(quizAttemptId)
                .orElseThrow(() -> new ResourceNoteFoundException("Quiz attempt not found with id: " + quizAttemptId));

        if (!attempt.getUser().getId().equals(managedUser.getId())) {
            throw new AccessDeniedException("You are not authorized to submit this quiz attempt.");
        }

        // 3. Verify attempt has not already been completed
        if (Boolean.TRUE.equals(attempt.getIsCompleted())) {
            throw new BadRequestException("Quiz attempt has already been completed.");
        }

        // 4. Check energy
        if (managedUser.getEnergy() < REQUIRED_ENERGY) {
            throw new BadRequestException("Insufficient energy to submit quiz. Minimum " + REQUIRED_ENERGY + " energy required.");
        }

        // 5. Verify request contains exactly 5 answers
        if (request == null || request.getAnswers() == null || request.getAnswers().size() != QUIZ_QUESTION_COUNT) {
            throw new BadRequestException("Quiz submission must contain exactly " + QUIZ_QUESTION_COUNT + " answers.");
        }

        // 6. Verify all questions belong to the attempt and are distinct
        Map<UUID, LessonQuestion> attemptQuestionsMap = attempt.getQuestions().stream()
                .collect(Collectors.toMap(LessonQuestion::getId, q -> q));

        Set<UUID> answeredQuestionIds = new HashSet<>();
        int correctCount = 0;

        List<UserAnswer> userAnswersToSave = new ArrayList<>();

        for (QuizAnswerItem answerItem : request.getAnswers()) {
            UUID questionId = answerItem.getQuestionId();
            if (questionId == null || !attemptQuestionsMap.containsKey(questionId)) {
                throw new BadRequestException("Question ID " + questionId + " does not belong to this quiz attempt.");
            }
            if (answeredQuestionIds.contains(questionId)) {
                throw new BadRequestException("Duplicate answer submitted for question ID: " + questionId);
            }
            answeredQuestionIds.add(questionId);

            LessonQuestion question = attemptQuestionsMap.get(questionId);

            boolean isCorrect = answerItem.getSelectedAnswer() != null
                    && answerItem.getSelectedAnswer().name().equalsIgnoreCase(question.getCorrectAnswer().trim());

            if (isCorrect) {
                correctCount++;
            }

            UserAnswer userAnswer = UserAnswer.builder()
                    .user(managedUser)
                    .quizAttempt(attempt)
                    .question(question)
                    .selectedAnswer(answerItem.getSelectedAnswer())
                    .isCorrect(isCorrect)
                    .build();

            userAnswersToSave.add(userAnswer);
        }

        if (answeredQuestionIds.size() != QUIZ_QUESTION_COUNT) {
            throw new BadRequestException("All " + QUIZ_QUESTION_COUNT + " questions in the attempt must be answered.");
        }

        userAnswerRepository.saveAll(userAnswersToSave);

        // 7. Calculate score percentage & XP
        int scorePercentage = (correctCount * 100) / QUIZ_QUESTION_COUNT;
        int baseXp = calculateBaseXp(correctCount);
        int bonusXp = calculateBonusXp(correctCount);
        int totalXpEarned = baseXp + bonusXp;

        // 8. Deduct 50 energy
        int newEnergy = Math.max(0, managedUser.getEnergy() - REQUIRED_ENERGY);
        managedUser.setEnergy(newEnergy);

        // 9. Update user total XP
        int currentXp = managedUser.getTotalXp() != null ? managedUser.getTotalXp() : 0;
        managedUser.setTotalXp(currentXp + totalXpEarned);
        userRepository.save(managedUser);

        // 10. Award XP to UserReward & create XpTransaction
        if (totalXpEarned > 0) {
            UserReward userReward = achievementService.getOrCreateUserReward(managedUser);
            userReward.addXp(totalXpEarned);
            userRewardRepository.save(userReward);

            XpTransaction xpTransaction = XpTransaction.builder()
                    .user(managedUser)
                    .activityType(ActivityType.QUIZ)
                    .referenceId(attempt.getId())
                    .points(totalXpEarned)
                    .build();
            xpTransactionRepository.save(xpTransaction);
        }

        // 11. Mark quiz attempt completed
        attempt.setCorrectAnswers(correctCount);
        attempt.setScore((double) scorePercentage);
        attempt.setBaseXp(baseXp);
        attempt.setBonusXp(bonusXp);
        attempt.setTotalXp(totalXpEarned);
        attempt.setIsCompleted(true);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt = quizAttemptRepository.save(attempt);

        return quizMapper.toQuizResultResponse(attempt, newEnergy);
    }

    @Override
    @Transactional
    public QuizResultResponse getQuizAttemptResult(UUID quizAttemptId, User user) {
        User managedUser = getUserOrThrow(user);
        checkAndResetDailyEnergy(managedUser);

        QuizAttempt attempt = quizAttemptRepository.findById(quizAttemptId)
                .orElseThrow(() -> new ResourceNoteFoundException("Quiz attempt not found with id: " + quizAttemptId));

        if (!attempt.getUser().getId().equals(managedUser.getId())) {
            throw new AccessDeniedException("You are not authorized to view this quiz attempt.");
        }

        return quizMapper.toQuizResultResponse(attempt, managedUser.getEnergy());
    }

    @Override
    @Transactional
    public QuizUserEnergyResponse getUserEnergyInfo(User user) {
        User managedUser = getUserOrThrow(user);
        checkAndResetDailyEnergy(managedUser);
        return quizMapper.toQuizUserEnergyResponse(managedUser);
    }

    /**
     * Resets energy to 100 if a new calendar day has started.
     */
    public void checkAndResetDailyEnergy(User user) {
        LocalDate currentDate = LocalDate.now();
        if (user.getEnergyResetDate() == null || !currentDate.equals(user.getEnergyResetDate())) {
            user.setEnergy(MAX_ENERGY);
            user.setEnergyResetDate(currentDate);
            userRepository.save(user);
        }
    }

    private User getUserOrThrow(User user) {
        if (user == null || user.getId() == null) {
            throw new ResourceNoteFoundException("User not authenticated.");
        }
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNoteFoundException("User not found with id: " + user.getId()));
    }

    private List<LessonQuestion> selectQuizQuestions(User user) {
        List<LessonQuestion> candidateQuestions = lessonQuestionRepository.findActiveQuestionsUnansweredByUser(user);
        Set<UUID> selectedIds = candidateQuestions.stream().map(LessonQuestion::getId).collect(Collectors.toSet());

        if (candidateQuestions.size() < QUIZ_QUESTION_COUNT) {
            List<LessonQuestion> extraQuestions = lessonQuestionRepository.findActiveQuestionsLeastRecentlyAnsweredByUser(user);
            for (LessonQuestion extra : extraQuestions) {
                if (!selectedIds.contains(extra.getId())) {
                    candidateQuestions.add(extra);
                    selectedIds.add(extra.getId());
                }
                if (candidateQuestions.size() >= QUIZ_QUESTION_COUNT) {
                    break;
                }
            }
        }

        if (candidateQuestions.size() < QUIZ_QUESTION_COUNT) {
            throw new BadRequestException("Not enough active quiz questions available in database. Required: " + QUIZ_QUESTION_COUNT);
        }

        return new ArrayList<>(candidateQuestions.subList(0, QUIZ_QUESTION_COUNT));
    }

    private int calculateBaseXp(int correctAnswers) {
        return correctAnswers * 10;
    }

    private int calculateBonusXp(int correctAnswers) {
        return switch (correctAnswers) {
            case 5 -> 20;
            case 4 -> 10;
            case 3 -> 5;
            default -> 0;
        };
    }
}
