package com.example.genz.community.chat.repository;

import com.example.genz.community.chat.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

    Optional<ChatConversation> findByDirectKey(String directKey);
}
