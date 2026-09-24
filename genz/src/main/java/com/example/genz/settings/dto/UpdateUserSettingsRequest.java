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
    @jakarta.validation.constraints.Pattern(regexp = "en|km", message = "must be en or km")
    private String language;
    @jakarta.validation.constraints.Pattern(regexp = "LIGHT|DARK|SYSTEM", message = "must be LIGHT, DARK or SYSTEM")
    private String theme;
    private Boolean billReminder;
    @jakarta.validation.constraints.Size(max = 2048)
    private String avatarUrl;
    private Boolean goalAlert;
    private Boolean lessonReminder;
    private Boolean aiTips;
    private Boolean dailyChallengeEnabled;
    private Boolean faceIdEnabled;
    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(60)
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
