package com.example.genz.community.chat.repository;

import com.example.genz.community.chat.entity.ChatMessageReceipt;
import com.example.genz.community.chat.entity.ChatMessageReceiptId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ChatMessageReceiptRepository
        extends JpaRepository<ChatMessageReceipt, ChatMessageReceiptId> {

    long countByConversation_IdAndUser_IdAndReadAtIsNull(UUID conversationId, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ChatMessageReceipt receipt
            SET receipt.deliveredAt = COALESCE(receipt.deliveredAt, :deliveredAt)
            WHERE receipt.message.id = :messageId
              AND receipt.user.id = :userId
            """)
    int markDelivered(
            @Param("messageId") UUID messageId,
            @Param("userId") UUID userId,
            @Param("deliveredAt") Instant deliveredAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ChatMessageReceipt receipt
            SET receipt.deliveredAt = COALESCE(receipt.deliveredAt, :readAt),
                receipt.readAt = :readAt
            WHERE receipt.conversation.id = :conversationId
              AND receipt.user.id = :userId
              AND receipt.readAt IS NULL
              AND receipt.message.createdAt <= :messageCreatedAt
            """)
    int markReadThrough(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId,
            @Param("messageCreatedAt") Instant messageCreatedAt,
            @Param("readAt") Instant readAt
    );
}
