package com.example.genz.notification.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private UUID id;
    private String title;
    private String message;
    private String type;
    private UUID referenceId;

    @JsonProperty("read")
    @JsonAlias({"isRead", "read"})
    private boolean isRead;

    private LocalDateTime createdAt;
}
