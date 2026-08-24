package com.example.genz.settings.dto;

import com.example.genz.community.post.enums.PostVisibility;
import com.example.genz.transaction.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserSettingsRequest {
    private String language;
    private String theme;
    private Boolean billReminder;
    private String avatarUrl;
    private Boolean goalAlert;
    private Boolean lessonReminder;
    private Boolean aiTips;
    private Boolean dailyChallengeEnabled;
    private Boolean faceIdEnabled;
    private Integer autoLockMinutes;
    private Boolean pushNotificationsEnabled;
    private Boolean smsNotificationsEnabled;
    private Boolean emailNotificationsEnabled;
    private PostVisibility profileVisibility;
    private Boolean showSavingsGoalsPublicly;
    private Boolean showFullNameOnPosts;
    private Boolean allowCommentsOnPosts;
    private Currency preferredCurrency;
}
