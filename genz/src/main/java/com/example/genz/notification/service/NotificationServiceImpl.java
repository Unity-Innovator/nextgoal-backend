package com.example.genz.notification.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.notification.dto.NotificationResponse;
import com.example.genz.notification.entity.Notification;
import com.example.genz.notification.enums.NotificationType;
import com.example.genz.notification.mapper.NotificationMapper;
import com.example.genz.notification.repository.NotificationRepository;
import com.example.genz.transaction.entity.GoalContribution;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final GoalMemberRepository goalMemberRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationWebSocketPublisher notificationWebSocketPublisher;

    @Override
    @Transactional
    public Notification createNotification(User user, String title, String message, NotificationType type, UUID referenceId) {
        Notification notification = notificationMapper.toEntity(user, title, message, type, referenceId);
        Notification savedNotification = notificationRepository.save(notification);
        Notification notificationToPublish = (savedNotification != null) ? savedNotification : notification;
        if (notificationToPublish.getCreatedAt() == null) {
            notificationToPublish.setCreatedAt(LocalDateTime.now());
        }

        if (notificationWebSocketPublisher != null) {
            String recipientPhone = null;
            if (user != null) {
                recipientPhone = user.getPhoneNumber();
                if ((recipientPhone == null || recipientPhone.isBlank()) && user.getId() != null) {
                    recipientPhone = userRepository.findById(user.getId())
                            .map(User::getPhoneNumber)
                            .orElse(null);
                }
            }
            NotificationResponse response = notificationMapper.toNotificationResponse(notificationToPublish);
            notificationWebSocketPublisher.sendToUser(recipientPhone, response);
        }

        return savedNotification;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found"));

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return notificationMapper.toNotificationResponseList(notifications);
    }

    @Override
    @Transactional
    public void markNotificationAsReadByReference(UUID referenceId, UUID userId) {
        notificationRepository.findByReferenceIdAndUserId(referenceId, userId)
                .ifPresent(notification -> {
                    notification.setRead(true);
                    notificationRepository.save(notification);
                });
    }

    @Override
    @Transactional
    public void createGoalContributionNotifications(Goal goal, GoalContribution contribution, User contributor) {
        if (goal == null || contribution == null || contributor == null) {
            return;
        }

        Map<UUID, User> recipientMap = new LinkedHashMap<>();

        if (goal.getOwner() != null) {
            recipientMap.put(goal.getOwner().getId(), goal.getOwner());
        }

        List<GoalMember> activeMembers = goalMemberRepository.findByGoalAndStatusWithUser(goal, MemberStatus.ACTIVE);
        if (activeMembers == null) {
            activeMembers = goalMemberRepository.findByGoalAndStatus(goal, MemberStatus.ACTIVE);
        }
        if (activeMembers != null) {
            for (GoalMember member : activeMembers) {
                if (member.getUser() != null) {
                    recipientMap.put(member.getUser().getId(), member.getUser());
                }
            }
        }

        recipientMap.put(contributor.getId(), contributor);

        String contributorName = (contributor.getFullName() != null && !contributor.getFullName().isBlank())
                ? contributor.getFullName()
                : "A member";

        BigDecimal amount = contribution.getAmount();
        String amountFormatted = (amount != null)
                ? amount.stripTrailingZeros().toPlainString()
                : "0";

        String title = "New Contribution";
        String message = String.format("%s contributed $%s to %s.", contributorName, amountFormatted, goal.getGoalName());

        for (User recipient : recipientMap.values()) {
            createNotification(recipient, title, message, NotificationType.GOAL_CONTRIBUTION, contribution.getId());
        }
    }
}
