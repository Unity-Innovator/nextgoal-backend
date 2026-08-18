package com.example.genz.community.chat.mapper;

import com.example.genz.community.chat.dto.ChatUserResponse;
import com.example.genz.community.chat.dto.ConversationMemberResponse;
import com.example.genz.community.chat.dto.ConversationResponse;
import com.example.genz.community.chat.dto.MessageAttachmentResponse;
import com.example.genz.community.chat.dto.MessageReceiptResponse;
import com.example.genz.community.chat.dto.MessageResponse;
import com.example.genz.community.chat.dto.MessageSummaryResponse;
import com.example.genz.community.chat.entity.ChatConversation;
import com.example.genz.community.chat.entity.ChatConversationMember;
import com.example.genz.community.chat.entity.ChatMessage;
import com.example.genz.community.chat.entity.ChatMessageAttachment;
import com.example.genz.community.chat.entity.ChatMessageReceipt;
import com.example.genz.community.chat.enums.ConversationMemberStatus;
import com.example.genz.community.chat.enums.ConversationType;
import com.example.genz.community.chat.enums.MessageStatus;
import com.example.genz.entity.Auth.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ChatMapper {

    public ConversationResponse toConversationResponse(
            ChatConversation conversation,
            UUID currentUserId,
            long unreadCount
    ) {
        List<ChatConversationMember> activeMembers = conversation.getMembers()
                .stream()
                .filter(member -> member.getStatus() == ConversationMemberStatus.ACTIVE)
                .toList();

        List<ConversationMemberResponse> members = activeMembers
                .stream()
                .map(this::toMemberResponse)
                .toList();

        String title = conversation.getTitle();
        String imageUrl = conversation.getImageUrl();
        if (conversation.getType() == ConversationType.DIRECT) {
            ChatConversationMember otherMember = activeMembers
                    .stream()
                    .filter(member -> !member.getUser().getId().equals(currentUserId))
                    .findFirst()
                    .orElse(null);
            if (otherMember != null) {
                title = otherMember.getUser().getFullName();
                imageUrl = avatarUrl(otherMember.getUser());
            }
        }

        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                title,
                imageUrl,
                members,
                toMessageSummary(conversation.getLastMessage()),
                unreadCount,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    public MessageResponse toMessageResponse(ChatMessage message) {
        boolean deleted = message.getStatus() == MessageStatus.DELETED;
        Map<String, Object> metadata = deleted || message.getMetadata() == null
                ? Collections.emptyMap()
                : new HashMap<>(message.getMetadata());

        List<MessageAttachmentResponse> attachments = deleted || message.getAttachments() == null
                ? List.of()
                : message.getAttachments().stream().map(this::toAttachmentResponse).toList();

        List<MessageReceiptResponse> receipts = message.getReceipts() == null
                ? List.of()
                : message.getReceipts().stream().map(this::toReceiptResponse).toList();

        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getClientMessageId(),
                message.getSender() == null ? null : toUserResponse(message.getSender()),
                message.getReplyToMessage() == null ? null : message.getReplyToMessage().getId(),
                message.getType(),
                deleted ? null : message.getBody(),
                metadata,
                message.getStatus(),
                attachments,
                receipts,
                message.getCreatedAt(),
                message.getEditedAt(),
                message.getDeletedAt()
        );
    }

    public ChatUserResponse toUserResponse(User user) {
        return new ChatUserResponse(user.getId(), user.getFullName(), avatarUrl(user));
    }

    private ConversationMemberResponse toMemberResponse(ChatConversationMember member) {
        return new ConversationMemberResponse(
                toUserResponse(member.getUser()),
                member.getRole(),
                member.getJoinedAt()
        );
    }

    private MessageSummaryResponse toMessageSummary(ChatMessage message) {
        if (message == null) {
            return null;
        }
        return new MessageSummaryResponse(
                message.getId(),
                message.getSender() == null ? null : message.getSender().getId(),
                message.getType(),
                message.getStatus() == MessageStatus.DELETED ? null : message.getBody(),
                message.getStatus(),
                message.getCreatedAt()
        );
    }

    private MessageAttachmentResponse toAttachmentResponse(ChatMessageAttachment attachment) {
        return new MessageAttachmentResponse(
                attachment.getId(),
                attachment.getType(),
                attachment.getFileUrl(),
                attachment.getThumbnailUrl(),
                attachment.getOriginalName(),
                attachment.getMimeType(),
                attachment.getFileSizeBytes(),
                attachment.getWidthPx(),
                attachment.getHeightPx(),
                attachment.getDurationMs()
        );
    }

    private MessageReceiptResponse toReceiptResponse(ChatMessageReceipt receipt) {
        return new MessageReceiptResponse(
                receipt.getUser().getId(),
                receipt.getDeliveredAt(),
                receipt.getReadAt()
        );
    }

    private String avatarUrl(User user) {
        return user.getSettings() == null ? null : user.getSettings().getAvatarUrl();
    }
}
