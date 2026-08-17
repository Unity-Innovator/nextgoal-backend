package com.example.genz.community.chat.service;

import com.example.genz.community.chat.dto.TypingEvent;
import com.example.genz.entity.Auth.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatTypingService {

    private final ChatAccessService accessService;

    @Transactional(readOnly = true)
    public TypingResult typing(
            UUID conversationId,
            boolean typing,
            String phoneNumber
    ) {
        User currentUser = accessService.requireUser(phoneNumber);
        accessService.requireActiveMember(conversationId, currentUser.getId());

        List<String> recipients = accessService.activeMembers(conversationId).stream()
                .filter(member -> !member.getUser().getId().equals(currentUser.getId()))
                .map(member -> member.getUser().getPhoneNumber())
                .distinct()
                .toList();
        TypingEvent event = new TypingEvent(
                "TYPING",
                conversationId,
                currentUser.getId(),
                typing
        );
        return new TypingResult(event, recipients);
    }
}
