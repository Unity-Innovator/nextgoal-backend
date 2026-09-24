package com.example.genz.community.chat.entity;

import com.example.genz.community.chat.enums.MessageStatus;
import com.example.genz.community.chat.enums.MessageType;
import com.example.genz.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "chat_messages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_chat_messages_conversation_message",
                        columnNames = {"conversation_id", "message_id"}
                ),
                @UniqueConstraint(
                        name = "ux_chat_messages_client_id",
                        columnNames = {"sender_id", "client_message_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "message_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private ChatMessage replyToMessage;

    @Column(name = "client_message_id", columnDefinition = "uuid")
    private UUID clientMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 15)
    private MessageType type = MessageType.TEXT;

    @Column(columnDefinition = "TEXT")
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageStatus status = MessageStatus.SENT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "message")
    private List<ChatMessageAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "message")
    private List<ChatMessageReaction> reactions = new ArrayList<>();

    @OneToMany(mappedBy = "message")
    private List<ChatMessageReceipt> receipts = new ArrayList<>();
}
