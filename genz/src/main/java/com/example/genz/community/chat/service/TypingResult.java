package com.example.genz.community.chat.service;

import com.example.genz.community.chat.dto.TypingEvent;

import java.util.List;

public record TypingResult(
        TypingEvent event,
        List<String> recipientUsernames
) {
}
