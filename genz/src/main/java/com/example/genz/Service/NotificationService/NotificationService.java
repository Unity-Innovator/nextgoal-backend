package com.example.genz.service.NotificationService;

import com.example.genz.dto.NotificationDTO.NotificationResponse;
import com.example.genz.entity.Auth.User;
import com.example.genz.entity.Notification.Notification;
import com.example.genz.enums.NotificationType;

import com.example.genz.entity.Goal.Goal;
import com.example.genz.entity.Transaction.GoalContribution;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    Notification createNotification(User user, String title, String message, NotificationType type, UUID referenceId);
    List<NotificationResponse> getUserNotifications(String phoneNumber);
    void markNotificationAsReadByReference(UUID referenceId, UUID userId);
    void createGoalContributionNotifications(Goal goal, GoalContribution contribution, User contributor);
}
