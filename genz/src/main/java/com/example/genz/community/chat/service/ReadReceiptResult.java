package com.example.genz.community.chat.service;

import com.example.genz.community.chat.dto.ReadReceiptEvent;

import java.util.List;

public record ReadReceiptResult(
        ReadReceiptEvent event,
        List<String> recipientUsernames
) {
}
