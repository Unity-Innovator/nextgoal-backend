package com.example.genz.community.chat.controller;

import com.example.genz.community.chat.dto.ChatErrorEvent;
import com.example.genz.community.chat.dto.MarkConversationReadRequest;
import com.example.genz.community.chat.dto.MarkMessageDeliveredRequest;
import com.example.genz.community.chat.dto.SendMessageRequest;
import com.example.genz.community.chat.dto.TypingRequest;
import com.example.genz.community.chat.exception.ChatBadRequestException;
import com.example.genz.community.chat.exception.ChatConflictException;
import com.example.genz.community.chat.exception.ChatNotFoundException;
import com.example.genz.community.chat.service.ChatMessageService;
import com.example.genz.community.chat.service.ChatRealtimePublisher;
import com.example.genz.community.chat.service.ChatReceiptService;
import com.example.genz.community.chat.service.ChatTypingService;
import com.example.genz.community.chat.service.DeliveryReceiptResult;
import com.example.genz.community.chat.service.MessageSendResult;
import com.example.genz.community.chat.service.ReadReceiptResult;
import com.example.genz.community.chat.service.TypingResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageService messageService;
    private final ChatReceiptService receiptService;
    private final ChatTypingService typingService;
    private final ChatRealtimePublisher realtimePublisher;

    @MessageMapping("/chat/{conversationId}/send")
    public void sendMessage(
            @DestinationVariable UUID conversationId,
            @Valid @Payload SendMessageRequest request,
            Principal principal
    ) {
        MessageSendResult result = messageService.sendMessage(
                conversationId,
                request,
                principal.getName()
        );
        realtimePublisher.publishMessage(result);
    }

    @MessageMapping("/chat/{conversationId}/delivered")
    public void markDelivered(
            @DestinationVariable UUID conversationId,
            @Valid @Payload MarkMessageDeliveredRequest request,
            Principal principal
    ) {
        DeliveryReceiptResult result = receiptService.markDelivered(
                conversationId,
                request.messageId(),
                principal.getName()
        );
        realtimePublisher.publishDeliveryReceipt(result);
    }

    @MessageMapping("/chat/{conversationId}/read")
    public void markRead(
            @DestinationVariable UUID conversationId,
            @Valid @Payload MarkConversationReadRequest request,
            Principal principal
    ) {
        ReadReceiptResult result = receiptService.markRead(
                conversationId,
                request.messageId(),
                principal.getName()
        );
        realtimePublisher.publishReadReceipt(result);
    }

    @MessageMapping("/chat/{conversationId}/typing")
    public void typing(
            @DestinationVariable UUID conversationId,
            @Payload TypingRequest request,
            Principal principal
    ) {
        TypingResult result = typingService.typing(
                conversationId,
                request.typing(),
                principal.getName()
        );
        realtimePublisher.publishTyping(result);
    }

    @MessageExceptionHandler({
            ChatBadRequestException.class,
            ChatConflictException.class,
            ChatNotFoundException.class,
            AccessDeniedException.class
    })
    @SendToUser(destinations = "/queue/chat/errors", broadcast = false)
    public ChatErrorEvent handleChatError(RuntimeException exception) {
        String code = exception instanceof AccessDeniedException
                ? "ACCESS_DENIED"
                : exception.getClass().getSimpleName();
        return new ChatErrorEvent("CHAT_ERROR", code, exception.getMessage(), Instant.now());
    }
}
