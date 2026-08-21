package com.example.genz.community.chat.entity;

import com.example.genz.community.chat.enums.ConversationMemberRole;
import com.example.genz.community.chat.enums.ConversationMemberStatus;
import com.example.genz.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "chat_conversation_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationMember {

    @EmbeddedId
    private ChatConversationMemberId id;

    @MapsId("conversationId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 10)
    private ConversationMemberRole role = ConversationMemberRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false, length = 10)
    private ConversationMemberStatus status = ConversationMemberStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private ChatMessage lastReadMessage;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    @Column(name = "muted_until")
    private Instant mutedUntil;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;
}
