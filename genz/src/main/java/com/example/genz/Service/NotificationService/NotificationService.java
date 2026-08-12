package com.example.genz.Service.NotificationService;

import com.example.genz.DTO.NotificationDTO.NotificationResponse;
import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Notification.Notification;
import com.example.genz.Enums.NotificationType;

import com.example.genz.Entity.Goal.Goal;
import com.example.genz.Entity.Transaction.GoalContribution;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    Notification createNotification(User user, String title, String message, NotificationType type, UUID referenceId);
    List<NotificationResponse> getUserNotifications(String phoneNumber);
    void markNotificationAsReadByReference(UUID referenceId, UUID userId);
    void createGoalContributionNotifications(Goal goal, GoalContribution contribution, User contributor);
}
