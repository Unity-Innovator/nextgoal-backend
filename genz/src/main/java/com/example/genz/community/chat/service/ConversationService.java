package com.example.genz.community.chat.service;

import com.example.genz.community.chat.dto.ChatSliceResponse;
import com.example.genz.community.chat.dto.ConversationResponse;
import com.example.genz.community.chat.dto.CreateGroupConversationRequest;
import com.example.genz.community.chat.entity.ChatConversation;
import com.example.genz.community.chat.entity.ChatConversationMember;
import com.example.genz.community.chat.entity.ChatConversationMemberId;
import com.example.genz.community.chat.enums.ConversationMemberRole;
import com.example.genz.community.chat.enums.ConversationMemberStatus;
import com.example.genz.community.chat.enums.ConversationType;
import com.example.genz.community.chat.exception.ChatBadRequestException;
import com.example.genz.community.chat.mapper.ChatMapper;
import com.example.genz.community.chat.repository.ChatConversationMemberRepository;
import com.example.genz.community.chat.repository.ChatConversationRepository;
import com.example.genz.community.chat.repository.ChatMessageReceiptRepository;
import com.example.genz.entity.Auth.User;
import com.example.genz.repository.AuthRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ChatAccessService accessService;
    private final UserRepository userRepository;
    private final ChatConversationRepository conversationRepository;
    private final ChatConversationMemberRepository memberRepository;
    private final ChatMessageReceiptRepository receiptRepository;
    private final ChatMapper chatMapper;

    @Transactional
    public ConversationResponse createDirectConversation(UUID otherUserId, String phoneNumber) {
        User currentUser = accessService.requireUser(phoneNumber);
        if (currentUser.getId().equals(otherUserId)) {
            throw new ChatBadRequestException("You cannot create a direct conversation with yourself");
        }

        User otherUser = accessService.requireUser(otherUserId);
        if (!accessService.areFriends(currentUser.getId(), otherUserId)) {
            throw new AccessDeniedException("Direct conversations can only be created with friends");
        }
        if (accessService.isBlockedEitherWay(currentUser.getId(), otherUserId)) {
            throw new AccessDeniedException("A blocked user cannot start a direct conversation");
        }

        String directKey = directKey(currentUser.getId(), otherUserId);
        ChatConversation existing = conversationRepository.findByDirectKey(directKey).orElse(null);
        if (existing != null) {
            accessService.requireActiveMember(existing.getId(), currentUser.getId());
            return mapConversation(existing, currentUser.getId());
        }

        ChatConversation conversation = new ChatConversation();
        conversation.setType(ConversationType.DIRECT);
        conversation.setCreatedBy(currentUser);
        conversation.setDirectKey(directKey);
        conversation = conversationRepository.saveAndFlush(conversation);

        List<ChatConversationMember> members = List.of(
                newMember(conversation, currentUser, ConversationMemberRole.MEMBER),
                newMember(conversation, otherUser, ConversationMemberRole.MEMBER)
        );
        memberRepository.saveAllAndFlush(members);
        conversation.setMembers(new ArrayList<>(members));

        return mapConversation(conversation, currentUser.getId());
    }

    @Transactional
    public ConversationResponse createGroupConversation(
            CreateGroupConversationRequest request,
            String phoneNumber
    ) {
        User currentUser = accessService.requireUser(phoneNumber);
        Set<UUID> memberIds = new HashSet<>(request.memberIds());
        memberIds.add(currentUser.getId());

        List<User> users = userRepository.findAllById(memberIds);
        if (users.size() != memberIds.size()) {
            Set<UUID> foundIds = users.stream().map(User::getId).collect(java.util.stream.Collectors.toSet());
            memberIds.removeAll(foundIds);
            throw new ChatBadRequestException("Unknown group member IDs: " + memberIds);
        }

        users.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .filter(user -> !accessService.areFriends(currentUser.getId(), user.getId()))
                .findFirst()
                .ifPresent(user -> {
                    throw new AccessDeniedException(
                            "Only friends can be added when creating a group conversation"
                    );
                });

        ChatConversation conversation = new ChatConversation();
        conversation.setType(ConversationType.GROUP);
        conversation.setTitle(request.title().strip());
        conversation.setImageUrl(blankToNull(request.imageUrl()));
        conversation.setCreatedBy(currentUser);
        ChatConversation savedConversation = conversationRepository.saveAndFlush(conversation);

        List<ChatConversationMember> members = users.stream()
                .map(user -> newMember(
                        savedConversation,
                        user,
                        user.getId().equals(currentUser.getId())
                                ? ConversationMemberRole.OWNER
                                : ConversationMemberRole.MEMBER
                ))
                .toList();
        memberRepository.saveAllAndFlush(members);
        savedConversation.setMembers(new ArrayList<>(members));

        return mapConversation(savedConversation, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public ChatSliceResponse<ConversationResponse> getInbox(
            String phoneNumber,
            int page,
            int size
    ) {
        validatePage(page, size);
        User currentUser = accessService.requireUser(phoneNumber);
        Slice<ChatConversationMember> inbox = memberRepository.findInbox(
                currentUser.getId(),
                ConversationMemberStatus.ACTIVE,
                PageRequest.of(page, size)
        );

        List<ConversationResponse> items = inbox.getContent().stream()
                .map(member -> mapConversation(member.getConversation(), currentUser.getId()))
                .toList();
        return new ChatSliceResponse<>(items, page, size, inbox.hasNext());
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID conversationId, String phoneNumber) {
        User currentUser = accessService.requireUser(phoneNumber);
        ChatConversationMember membership = accessService.requireActiveMember(
                conversationId,
                currentUser.getId()
        );
        return mapConversation(membership.getConversation(), currentUser.getId());
    }

    private ConversationResponse mapConversation(ChatConversation conversation, UUID currentUserId) {
        long unreadCount = receiptRepository.countByConversation_IdAndUser_IdAndReadAtIsNull(
                conversation.getId(),
                currentUserId
        );
        return chatMapper.toConversationResponse(conversation, currentUserId, unreadCount);
    }

    private ChatConversationMember newMember(
            ChatConversation conversation,
            User user,
            ConversationMemberRole role
    ) {
        ChatConversationMember member = new ChatConversationMember();
        member.setId(new ChatConversationMemberId(conversation.getId(), user.getId()));
        member.setConversation(conversation);
        member.setUser(user);
        member.setRole(role);
        member.setStatus(ConversationMemberStatus.ACTIVE);
        return member;
    }

    private String directKey(UUID firstUserId, UUID secondUserId) {
        if (firstUserId.compareTo(secondUserId) < 0) {
            return firstUserId + ":" + secondUserId;
        }
        return secondUserId + ":" + firstUserId;
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ChatBadRequestException("Page must be >= 0 and size must be between 1 and 100");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
