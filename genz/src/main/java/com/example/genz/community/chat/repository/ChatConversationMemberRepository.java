package com.example.genz.community.chat.repository;

import com.example.genz.community.chat.entity.ChatConversationMember;
import com.example.genz.community.chat.entity.ChatConversationMemberId;
import com.example.genz.community.chat.enums.ConversationMemberStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatConversationMemberRepository
        extends JpaRepository<ChatConversationMember, ChatConversationMemberId> {

    Optional<ChatConversationMember> findByConversation_IdAndUser_Id(
            UUID conversationId,
            UUID userId
    );

    Optional<ChatConversationMember> findByConversation_IdAndUser_IdAndStatus(
            UUID conversationId,
            UUID userId,
            ConversationMemberStatus status
    );

    List<ChatConversationMember> findByConversation_IdAndStatus(
            UUID conversationId,
            ConversationMemberStatus status
    );

    @Query("""
            SELECT member
            FROM ChatConversationMember member
            JOIN FETCH member.conversation conversation
            WHERE member.user.id = :userId
              AND member.status = :status
            ORDER BY
              CASE WHEN conversation.lastMessageAt IS NULL THEN 1 ELSE 0 END,
              conversation.lastMessageAt DESC,
              conversation.createdAt DESC
            """)
    Slice<ChatConversationMember> findInbox(
            @Param("userId") UUID userId,
            @Param("status") ConversationMemberStatus status,
            Pageable pageable
    );
}
