package com.example.genz.Service.NotificationService;

import com.example.genz.DTO.NotificationDTO.NotificationResponse;
import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Notification.Notification;
import com.example.genz.Enums.NotificationType;
import com.example.genz.Entity.Goal.Goal;
import com.example.genz.Entity.Goal.GoalMember;
import com.example.genz.Entity.Transaction.GoalContribution;
import com.example.genz.Enums.MemberStatus;
import com.example.genz.mapper.NotificationMapper;
import com.example.genz.Repository.AuthRepository.UserRepository;
import com.example.genz.Repository.GoalRepository.GoalMemberRepository;
import com.example.genz.Repository.NotificationRepository.NotificationRepository;
import com.example.genz.exception.ResourceNoteFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final GoalMemberRepository goalMemberRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public Notification createNotification(User user, String title, String message, NotificationType type, UUID referenceId) {
        Notification notification = notificationMapper.toEntity(user, title, message, type, referenceId);
        return notificationRepository.save(notification);
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
