package com.example.genz.community.chat.dto;

public record MessageEvent(
        String event,
        MessageResponse message,
        boolean duplicate
) {
}
