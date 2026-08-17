package com.example.genz.community.chat.service;

import com.example.genz.community.chat.dto.DeliveryReceiptEvent;
import com.example.genz.community.chat.dto.ReadReceiptEvent;
import com.example.genz.community.chat.entity.ChatConversationMember;
import com.example.genz.community.chat.entity.ChatMessage;
import com.example.genz.community.chat.exception.ChatBadRequestException;
import com.example.genz.community.chat.repository.ChatConversationMemberRepository;
import com.example.genz.community.chat.repository.ChatMessageReceiptRepository;
import com.example.genz.community.chat.repository.ChatMessageRepository;
import com.example.genz.entity.Auth.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatReceiptService {

    private final ChatAccessService accessService;
    private final ChatMessageRepository messageRepository;
    private final ChatMessageReceiptRepository receiptRepository;
    private final ChatConversationMemberRepository memberRepository;

    @Transactional
    public DeliveryReceiptResult markDelivered(
            UUID conversationId,
            UUID messageId,
            String phoneNumber
    ) {
        User currentUser = accessService.requireUser(phoneNumber);
        accessService.requireActiveMember(conversationId, currentUser.getId());
        requireMessage(conversationId, messageId);

        Instant deliveredAt = Instant.now();
        int updated = receiptRepository.markDelivered(messageId, currentUser.getId(), deliveredAt);
        if (updated == 0) {
            throw new ChatBadRequestException("No delivery receipt exists for this user and message");
        }

        DeliveryReceiptEvent event = new DeliveryReceiptEvent(
                "MESSAGE_DELIVERED",
                conversationId,
                messageId,
                currentUser.getId(),
                deliveredAt
        );
        return new DeliveryReceiptResult(event, activeMemberUsernames(conversationId));
    }

    @Transactional
    public ReadReceiptResult markRead(
            UUID conversationId,
            UUID messageId,
            String phoneNumber
    ) {
        User currentUser = accessService.requireUser(phoneNumber);
        ChatConversationMember membership = accessService.requireActiveMember(
                conversationId,
                currentUser.getId()
        );
        ChatMessage requestedMessage = requireMessage(conversationId, messageId);
        ChatMessage effectiveMessage = requestedMessage;

        if (membership.getLastReadMessage() != null
                && membership.getLastReadMessage().getCreatedAt().isAfter(requestedMessage.getCreatedAt())) {
            effectiveMessage = membership.getLastReadMessage();
        }

        Instant readAt = Instant.now();
        receiptRepository.markReadThrough(
                conversationId,
                currentUser.getId(),
                effectiveMessage.getCreatedAt(),
                readAt
        );
        membership.setLastReadMessage(effectiveMessage);
        membership.setLastReadAt(readAt);
        memberRepository.save(membership);

        ReadReceiptEvent event = new ReadReceiptEvent(
                "MESSAGES_READ",
                conversationId,
                effectiveMessage.getId(),
                currentUser.getId(),
                readAt
        );
        return new ReadReceiptResult(event, activeMemberUsernames(conversationId));
    }

    private ChatMessage requireMessage(UUID conversationId, UUID messageId) {
        return messageRepository.findByIdAndConversation_Id(messageId, conversationId)
                .orElseThrow(() -> new ChatBadRequestException(
                        "Message does not belong to this conversation"
                ));
    }

    private List<String> activeMemberUsernames(UUID conversationId) {
        return accessService.activeMembers(conversationId).stream()
                .map(member -> member.getUser().getPhoneNumber())
                .distinct()
                .toList();
    }
}
