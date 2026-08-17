package com.example.genz.community.chat.service;

import com.example.genz.community.chat.dto.DeliveryReceiptEvent;

import java.util.List;

public record DeliveryReceiptResult(
        DeliveryReceiptEvent event,
        List<String> recipientUsernames
) {
}
