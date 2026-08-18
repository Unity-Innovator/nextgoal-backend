package com.example.genz.community.chat.service;

import com.example.genz.community.chat.dto.MessageResponse;

import java.util.List;

public record MessageSendResult(
        MessageResponse message,
        boolean duplicate,
        List<String> recipientUsernames
) {
}
