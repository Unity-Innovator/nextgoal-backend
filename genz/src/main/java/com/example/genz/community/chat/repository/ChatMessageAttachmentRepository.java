package com.example.genz.community.chat.repository;

import com.example.genz.community.chat.entity.ChatMessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatMessageAttachmentRepository extends JpaRepository<ChatMessageAttachment, UUID> {
}
