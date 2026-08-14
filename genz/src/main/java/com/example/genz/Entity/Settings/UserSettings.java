package com.example.genz.entity.Settings;

import com.example.genz.entity.Auth.User;
import com.example.genz.enums.Currency;
import com.example.genz.enums.PostVisibility;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "settings_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String theme;

    @Column(name = "bill_reminder", nullable = false)
    private Boolean billReminder;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "goal_alert", nullable = false)
    private Boolean goalAlert;

    @Column(name = "lesson_reminder", nullable = false)
    private Boolean lessonReminder;

    @Column(name = "ai_tips", nullable = false)
    private Boolean aiTips;

    @Column(name = "face_id_enabled", nullable = false)
    private Boolean faceIdEnabled;

    @Column(name = "auto_lock_minutes", nullable = false)
    private Integer autoLockMinutes;

    @Column(name = "push_notifications_enabled", nullable = false)
    private Boolean pushNotificationsEnabled;

    @Column(name = "sms_notifications_enabled", nullable = false)
    private Boolean smsNotificationsEnabled;

    @Column(name = "email_notifications_enabled", nullable = false)
    private Boolean emailNotificationsEnabled;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "profile_visibility", nullable = false)
    private PostVisibility profileVisibility;

    @Column(name = "show_savings_goals_publicly", nullable = false)
    private Boolean showSavingsGoalsPublicly;

    @Column(name = "show_full_name_on_posts", nullable = false)
    private Boolean showFullNameOnPosts;

    @Column(name = "allow_comments_on_posts", nullable = false)
    private Boolean allowCommentsOnPosts;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_currency", nullable = false)
    private Currency preferredCurrency;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
