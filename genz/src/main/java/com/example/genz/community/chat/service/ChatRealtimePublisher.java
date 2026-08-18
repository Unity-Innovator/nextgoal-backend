package com.example.genz.community.chat.service;

import com.example.genz.community.chat.dto.DeliveryReceiptEvent;
import com.example.genz.community.chat.dto.MessageEvent;
import com.example.genz.community.chat.dto.ReadReceiptEvent;
import com.example.genz.community.chat.dto.TypingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRealtimePublisher {

    private static final String MESSAGE_QUEUE = "/queue/chat/messages";
    private static final String RECEIPT_QUEUE = "/queue/chat/receipts";
    private static final String TYPING_QUEUE = "/queue/chat/typing";

    private final SimpMessagingTemplate messagingTemplate;

    public void publishMessage(MessageSendResult result) {
        MessageEvent event = new MessageEvent("MESSAGE_CREATED", result.message(), result.duplicate());
        sendToUsers(result.recipientUsernames(), MESSAGE_QUEUE, event);
    }

    public void publishDeliveryReceipt(DeliveryReceiptResult result) {
        DeliveryReceiptEvent event = result.event();
        sendToUsers(result.recipientUsernames(), RECEIPT_QUEUE, event);
    }

    public void publishReadReceipt(ReadReceiptResult result) {
        ReadReceiptEvent event = result.event();
        sendToUsers(result.recipientUsernames(), RECEIPT_QUEUE, event);
    }

    public void publishTyping(TypingResult result) {
        TypingEvent event = result.event();
        sendToUsers(result.recipientUsernames(), TYPING_QUEUE, event);
    }

    private void sendToUsers(List<String> usernames, String destination, Object payload) {
        usernames.stream().distinct().forEach(username ->
                messagingTemplate.convertAndSendToUser(username, destination, payload)
        );
    }
}
