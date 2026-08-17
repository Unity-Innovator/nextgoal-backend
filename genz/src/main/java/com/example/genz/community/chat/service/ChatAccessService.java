package com.example.genz.community.chat.service;

import com.example.genz.community.chat.entity.ChatConversation;
import com.example.genz.community.chat.entity.ChatConversationMember;
import com.example.genz.community.chat.enums.ConversationMemberStatus;
import com.example.genz.community.chat.exception.ChatNotFoundException;
import com.example.genz.community.chat.repository.ChatConversationMemberRepository;
import com.example.genz.community.chat.repository.ChatConversationRepository;
import com.example.genz.community.friendship.repository.FriendshipRepository;
import com.example.genz.community.friendship.repository.UserBlockRepository;
import com.example.genz.entity.Auth.User;
import com.example.genz.repository.AuthRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatAccessService {

    private final UserRepository userRepository;
    private final ChatConversationRepository conversationRepository;
    private final ChatConversationMemberRepository memberRepository;
    private final UserBlockRepository userBlockRepository;
    private final FriendshipRepository friendshipRepository;

    @Transactional(readOnly = true)
    public User requireUser(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ChatNotFoundException("Authenticated user was not found"));
    }

    @Transactional(readOnly = true)
    public User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ChatNotFoundException("User not found: " + userId));
    }

    @Transactional(readOnly = true)
    public ChatConversation requireConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ChatNotFoundException(
                        "Conversation not found: " + conversationId
                ));
    }

    @Transactional(readOnly = true)
    public ChatConversationMember requireActiveMember(UUID conversationId, UUID userId) {
        return memberRepository.findByConversation_IdAndUser_IdAndStatus(
                        conversationId,
                        userId,
                        ConversationMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new AccessDeniedException(
                        "You are not an active member of this conversation"
                ));
    }

    @Transactional(readOnly = true)
    public List<ChatConversationMember> activeMembers(UUID conversationId) {
        return memberRepository.findByConversation_IdAndStatus(
                conversationId,
                ConversationMemberStatus.ACTIVE
        );
    }

    @Transactional(readOnly = true)
    public boolean isBlockedEitherWay(UUID firstUserId, UUID secondUserId) {
        return userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(firstUserId, secondUserId)
                || userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(secondUserId, firstUserId);
    }

    @Transactional(readOnly = true)
    public boolean areFriends(UUID firstUserId, UUID secondUserId) {
        if (firstUserId.compareTo(secondUserId) < 0) {
            return friendshipRepository.existsByUserOne_IdAndUserTwo_Id(firstUserId, secondUserId);
        }
        return friendshipRepository.existsByUserOne_IdAndUserTwo_Id(secondUserId, firstUserId);
    }
}
