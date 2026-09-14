package com.example.genz.community.chat.service;

import com.example.genz.community.chat.dto.ChatSliceResponse;
import com.example.genz.community.chat.dto.MessageResponse;
import com.example.genz.community.chat.dto.SendAttachmentRequest;
import com.example.genz.community.chat.dto.SendMessageRequest;
import com.example.genz.community.chat.entity.ChatConversation;
import com.example.genz.community.chat.entity.ChatConversationMember;
import com.example.genz.community.chat.entity.ChatMessage;
import com.example.genz.community.chat.entity.ChatMessageAttachment;
import com.example.genz.community.chat.entity.ChatMessageReceipt;
import com.example.genz.community.chat.entity.ChatMessageReceiptId;
import com.example.genz.community.chat.enums.ConversationType;
import com.example.genz.community.chat.enums.AttachmentType;
import com.example.genz.community.chat.enums.MessageStatus;
import com.example.genz.community.chat.enums.MessageType;
import com.example.genz.community.chat.exception.ChatBadRequestException;
import com.example.genz.community.chat.mapper.ChatMapper;
import com.example.genz.community.chat.repository.ChatConversationRepository;
import com.example.genz.community.chat.repository.ChatMessageReceiptRepository;
import com.example.genz.community.chat.repository.ChatMessageAttachmentRepository;
import com.example.genz.community.chat.repository.ChatMessageRepository;
import com.example.genz.auth.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final long MAX_ATTACHMENT_SIZE_BYTES = 40L * 1024 * 1024;

    private final ChatAccessService accessService;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMessageAttachmentRepository attachmentRepository;
    private final ChatMessageReceiptRepository receiptRepository;
    private final ChatMapper chatMapper;
    private final SupabaseStorageService storageService;

    @Transactional
    public MessageSendResult sendMessage(
            UUID conversationId,
            SendMessageRequest request,
            String phoneNumber
    ) {
        User sender = accessService.requireUser(phoneNumber);
        ChatConversationMember senderMembership = accessService.requireActiveMember(
                conversationId,
                sender.getId()
        );
        ChatConversation conversation = senderMembership.getConversation();
        assertDirectConversationIsNotBlocked(conversation, sender.getId());

        ChatMessage existing = messageRepository.findBySender_IdAndClientMessageId(
                sender.getId(),
                request.clientMessageId()
        ).orElse(null);
        if (existing != null) {
            if (!existing.getConversation().getId().equals(conversationId)) {
                throw new ChatBadRequestException(
                        "clientMessageId has already been used in another conversation"
                );
            }
            return new MessageSendResult(
                    chatMapper.toMessageResponse(existing),
                    true,
                    List.of(phoneNumber)
            );
        }

        ChatMessage replyTo = null;
        if (request.replyToMessageId() != null) {
            replyTo = messageRepository.findByIdAndConversation_Id(
                            request.replyToMessageId(),
                            conversationId
                    )
                    .orElseThrow(() -> new ChatBadRequestException(
                            "The replied-to message does not belong to this conversation"
                    ));
        }

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setReplyToMessage(replyTo);
        message.setClientMessageId(request.clientMessageId());
        message.setType(MessageType.TEXT);
        message.setBody(request.body().strip());
        message.setMetadata(new HashMap<>());
        message.setStatus(MessageStatus.SENT);
        ChatMessage savedMessage = messageRepository.saveAndFlush(message);

        List<ChatConversationMember> activeMembers = accessService.activeMembers(conversationId);
        List<ChatMessageReceipt> receipts = activeMembers.stream()
                .filter(member -> !member.getUser().getId().equals(sender.getId()))
                .map(member -> newReceipt(savedMessage, member.getUser()))
                .toList();
        receiptRepository.saveAll(receipts);
        savedMessage.setReceipts(new ArrayList<>(receipts));

        conversation.setLastMessage(savedMessage);
        conversation.setLastMessageAt(savedMessage.getCreatedAt());
        conversationRepository.save(conversation);

        List<String> recipients = activeMembers.stream()
                .map(member -> member.getUser().getPhoneNumber())
                .distinct()
                .toList();
        return new MessageSendResult(
                chatMapper.toMessageResponse(savedMessage),
                false,
                recipients
        );
    }

    @Transactional
    public MessageSendResult sendAttachment(
            UUID conversationId,
            SendAttachmentRequest request,
            String phoneNumber
    ) {
        validateAttachment(request);
        User sender = accessService.requireUser(phoneNumber);
        ChatConversationMember senderMembership = accessService.requireActiveMember(
                conversationId,
                sender.getId()
        );
        ChatConversation conversation = senderMembership.getConversation();
        assertDirectConversationIsNotBlocked(conversation, sender.getId());

        ChatMessage existing = messageRepository.findBySender_IdAndClientMessageId(
                sender.getId(),
                request.getClientMessageId()
        ).orElse(null);
        if (existing != null) {
            if (!existing.getConversation().getId().equals(conversationId)) {
                throw new ChatBadRequestException(
                        "clientMessageId has already been used in another conversation"
                );
            }
            return new MessageSendResult(
                    chatMapper.toMessageResponse(existing),
                    true,
                    List.of(phoneNumber)
            );
        }

        ChatMessage replyTo = null;
        if (request.getReplyToMessageId() != null) {
            replyTo = messageRepository.findByIdAndConversation_Id(
                            request.getReplyToMessageId(),
                            conversationId
                    )
                    .orElseThrow(() -> new ChatBadRequestException(
                            "The replied-to message does not belong to this conversation"
                    ));
        }

        AttachmentType attachmentType = attachmentType(request);
        String storagePath = storagePath(conversationId, sender.getId(), request);
        boolean uploaded = false;
        try {
            storageService.upload(request.getFile(), storagePath, attachmentType);
            uploaded = true;
            String signedUrl = storageService.createSignedUrl(storagePath);

            ChatMessage message = new ChatMessage();
            message.setConversation(conversation);
            message.setSender(sender);
            message.setReplyToMessage(replyTo);
            message.setClientMessageId(request.getClientMessageId());
            message.setType(messageType(attachmentType));
            message.setBody(normalizedBody(request.getBody()));
            message.setMetadata(new HashMap<>());
            message.setStatus(MessageStatus.SENT);
            ChatMessage savedMessage = messageRepository.saveAndFlush(message);

            ChatMessageAttachment attachment = new ChatMessageAttachment();
            attachment.setMessage(savedMessage);
            attachment.setType(attachmentType);
            attachment.setStoragePath(storagePath);
            attachment.setFileUrl(signedUrl);
            attachment.setOriginalName(safeFileName(request));
            attachment.setMimeType(request.getFile().getContentType());
            attachment.setFileSizeBytes(request.getFile().getSize());
            attachmentRepository.saveAndFlush(attachment);
            savedMessage.setAttachments(new ArrayList<>(List.of(attachment)));

            List<ChatConversationMember> activeMembers = accessService.activeMembers(conversationId);
            List<ChatMessageReceipt> receipts = activeMembers.stream()
                    .filter(member -> !member.getUser().getId().equals(sender.getId()))
                    .map(member -> newReceipt(savedMessage, member.getUser()))
                    .toList();
            receiptRepository.saveAll(receipts);
            savedMessage.setReceipts(new ArrayList<>(receipts));

            conversation.setLastMessage(savedMessage);
            conversation.setLastMessageAt(savedMessage.getCreatedAt());
            conversationRepository.save(conversation);

            List<String> recipients = activeMembers.stream()
                    .map(member -> member.getUser().getPhoneNumber())
                    .distinct()
                    .toList();
            return new MessageSendResult(
                    chatMapper.toMessageResponse(savedMessage),
                    false,
                    recipients
            );
        } catch (RuntimeException exception) {
            if (uploaded) {
                storageService.deleteQuietly(storagePath);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ChatSliceResponse<MessageResponse> getHistory(
            UUID conversationId,
            String phoneNumber,
            int page,
            int size
    ) {
        validatePage(page, size);
        User currentUser = accessService.requireUser(phoneNumber);
        accessService.requireActiveMember(conversationId, currentUser.getId());

        Slice<ChatMessage> history = messageRepository.findHistory(
                conversationId,
                PageRequest.of(page, size)
        );
        List<MessageResponse> items = history.getContent().stream()
                .map(chatMapper::toMessageResponse)
                .toList();
        return new ChatSliceResponse<>(items, page, size, history.hasNext());
    }

    private ChatMessageReceipt newReceipt(ChatMessage message, User recipient) {
        ChatMessageReceipt receipt = new ChatMessageReceipt();
        receipt.setId(new ChatMessageReceiptId(message.getId(), recipient.getId()));
        receipt.setConversation(message.getConversation());
        receipt.setMessage(message);
        receipt.setUser(recipient);
        return receipt;
    }

    private void assertDirectConversationIsNotBlocked(ChatConversation conversation, UUID senderId) {
        if (conversation.getType() != ConversationType.DIRECT) {
            return;
        }
        UUID otherUserId = accessService.activeMembers(conversation.getId())
                .stream()
                .map(member -> member.getUser().getId())
                .filter(userId -> !userId.equals(senderId))
                .findFirst()
                .orElseThrow(() -> new ChatBadRequestException(
                        "Direct conversation does not have another active member"
                ));
        if (accessService.isBlockedEitherWay(senderId, otherUserId)) {
            throw new AccessDeniedException("Messages cannot be sent to a blocked user");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ChatBadRequestException("Page must be >= 0 and size must be between 1 and 100");
        }
    }

    private void validateAttachment(SendAttachmentRequest request) {
        if (request.getFile() == null || request.getFile().isEmpty()) {
            throw new ChatBadRequestException("An attachment file is required");
        }
        if (request.getFile().getSize() > MAX_ATTACHMENT_SIZE_BYTES) {
            throw new ChatBadRequestException("Attachment must not exceed 40 MB");
        }
    }

    private AttachmentType attachmentType(SendAttachmentRequest request) {
        String contentType = request.getFile().getContentType();
        if (contentType == null) {
            return AttachmentType.FILE;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("image/") && !normalized.equals("image/svg+xml")) {
            return AttachmentType.IMAGE;
        }
        if (normalized.startsWith("video/")) {
            return AttachmentType.VIDEO;
        }
        if (normalized.startsWith("audio/")) {
            return AttachmentType.VOICE;
        }
        return AttachmentType.FILE;
    }

    private MessageType messageType(AttachmentType attachmentType) {
        return switch (attachmentType) {
            case IMAGE -> MessageType.IMAGE;
            case VIDEO -> MessageType.VIDEO;
            case VOICE -> MessageType.VOICE;
            case FILE -> MessageType.FILE;
        };
    }

    private String storagePath(UUID conversationId, UUID senderId, SendAttachmentRequest request) {
        String fileName = safeFileName(request);
        int extensionStart = fileName.lastIndexOf('.');
        String extension = extensionStart > 0 && extensionStart < fileName.length() - 1
                ? fileName.substring(extensionStart).replaceAll("[^A-Za-z0-9.]", "")
                : "";
        if (extension.length() > 11) {
            extension = "";
        }
        return "chat/" + conversationId + "/" + senderId + "/" + UUID.randomUUID() + extension;
    }

    private String safeFileName(SendAttachmentRequest request) {
        String fileName = request.getFile().getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            return "attachment";
        }
        String leafName = fileName.replace('\\', '/');
        leafName = leafName.substring(leafName.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_")
                .trim();
        return leafName.isBlank() ? "attachment" : leafName.substring(0, Math.min(255, leafName.length()));
    }

    private String normalizedBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        return body.strip();
    }
}
