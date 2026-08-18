package com.example.genz.community.chat.repository;

import com.example.genz.community.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Optional<ChatMessage> findBySender_IdAndClientMessageId(UUID senderId, UUID clientMessageId);

    Optional<ChatMessage> findByIdAndConversation_Id(UUID messageId, UUID conversationId);

    @Query("""
            SELECT message
            FROM ChatMessage message
            WHERE message.conversation.id = :conversationId
            ORDER BY message.createdAt DESC, message.id DESC
            """)
    Slice<ChatMessage> findHistory(
            @Param("conversationId") UUID conversationId,
            Pageable pageable
    );
}
