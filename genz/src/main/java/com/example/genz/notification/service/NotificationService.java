package com.example.genz.notification.service;

import com.example.genz.notification.dto.NotificationResponse;
import com.example.genz.auth.entity.User;
import com.example.genz.notification.entity.Notification;
import com.example.genz.notification.enums.NotificationType;

import com.example.genz.goal.entity.Goal;
import com.example.genz.transaction.entity.GoalContribution;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    Notification createNotification(User user, String title, String message, NotificationType type, UUID referenceId);
    List<NotificationResponse> getUserNotifications(String phoneNumber);
    void markNotificationAsReadByReference(UUID referenceId, UUID userId);
    void createGoalContributionNotifications(Goal goal, GoalContribution contribution, User contributor);
}
