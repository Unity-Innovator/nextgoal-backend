package com.example.genz.community.chat.controller;

import com.example.genz.community.chat.dto.ChatSliceResponse;
import com.example.genz.community.chat.dto.ConversationResponse;
import com.example.genz.community.chat.dto.CreateDirectConversationRequest;
import com.example.genz.community.chat.dto.CreateGroupConversationRequest;
import com.example.genz.community.chat.dto.DeliveryReceiptEvent;
import com.example.genz.community.chat.dto.MarkConversationReadRequest;
import com.example.genz.community.chat.dto.MarkMessageDeliveredRequest;
import com.example.genz.community.chat.dto.MessageResponse;
import com.example.genz.community.chat.dto.ReadReceiptEvent;
import com.example.genz.community.chat.dto.SendMessageRequest;
import com.example.genz.community.chat.service.ChatMessageService;
import com.example.genz.community.chat.service.ChatRealtimePublisher;
import com.example.genz.community.chat.service.ChatReceiptService;
import com.example.genz.community.chat.service.ConversationService;
import com.example.genz.community.chat.service.DeliveryReceiptResult;
import com.example.genz.community.chat.service.MessageSendResult;
import com.example.genz.community.chat.service.ReadReceiptResult;
import com.example.genz.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat/conversations")
@RequiredArgsConstructor
public class ChatController {

    private final ConversationService conversationService;
    private final ChatMessageService messageService;
    private final ChatReceiptService receiptService;
    private final ChatRealtimePublisher realtimePublisher;

    @PostMapping("/direct")
    public ResponseEntity<ApiResponse<ConversationResponse>> createDirectConversation(
            @Valid @RequestBody CreateDirectConversationRequest request,
            Authentication authentication
    ) {
        ConversationResponse response = conversationService.createDirectConversation(
                request.userId(),
                authentication.getName()
        );
        return ResponseEntity.ok(ApiResponse.success("Direct conversation ready", response));
    }

    @PostMapping("/group")
    public ResponseEntity<ApiResponse<ConversationResponse>> createGroupConversation(
            @Valid @RequestBody CreateGroupConversationRequest request,
            Authentication authentication
    ) {
        ConversationResponse response = conversationService.createGroupConversation(
                request,
                authentication.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Group conversation created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ChatSliceResponse<ConversationResponse>>> getInbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication authentication
    ) {
        ChatSliceResponse<ConversationResponse> response = conversationService.getInbox(
                authentication.getName(),
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved", response));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @PathVariable UUID conversationId,
            Authentication authentication
    ) {
        ConversationResponse response = conversationService.getConversation(
                conversationId,
                authentication.getName()
        );
        return ResponseEntity.ok(ApiResponse.success("Conversation retrieved", response));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<ChatSliceResponse<MessageResponse>>> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication
    ) {
        ChatSliceResponse<MessageResponse> response = messageService.getHistory(
                conversationId,
                authentication.getName(),
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved", response));
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        MessageSendResult result = messageService.sendMessage(
                conversationId,
                request,
                authentication.getName()
        );
        realtimePublisher.publishMessage(result);
        return ResponseEntity.status(result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(ApiResponse.success(
                        result.duplicate() ? "Message already accepted" : "Message sent",
                        result.message()
                ));
    }

    @PostMapping("/{conversationId}/delivered")
    public ResponseEntity<ApiResponse<DeliveryReceiptEvent>> markDelivered(
            @PathVariable UUID conversationId,
            @Valid @RequestBody MarkMessageDeliveredRequest request,
            Authentication authentication
    ) {
        DeliveryReceiptResult result = receiptService.markDelivered(
                conversationId,
                request.messageId(),
                authentication.getName()
        );
        realtimePublisher.publishDeliveryReceipt(result);
        return ResponseEntity.ok(ApiResponse.success("Message marked as delivered", result.event()));
    }

    @PostMapping("/{conversationId}/read")
    public ResponseEntity<ApiResponse<ReadReceiptEvent>> markRead(
            @PathVariable UUID conversationId,
            @Valid @RequestBody MarkConversationReadRequest request,
            Authentication authentication
    ) {
        ReadReceiptResult result = receiptService.markRead(
                conversationId,
                request.messageId(),
                authentication.getName()
        );
        realtimePublisher.publishReadReceipt(result);
        return ResponseEntity.ok(ApiResponse.success("Conversation read position updated", result.event()));
    }
}
