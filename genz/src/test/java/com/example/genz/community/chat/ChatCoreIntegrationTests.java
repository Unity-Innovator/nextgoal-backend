package com.example.genz.community.chat;

import com.example.genz.community.chat.dto.ChatSliceResponse;
import com.example.genz.community.chat.dto.ConversationResponse;
import com.example.genz.community.chat.dto.MessageResponse;
import com.example.genz.community.chat.dto.SendMessageRequest;
import com.example.genz.community.chat.repository.ChatMessageReceiptRepository;
import com.example.genz.community.chat.service.ChatMessageService;
import com.example.genz.community.chat.service.ChatReceiptService;
import com.example.genz.community.chat.service.ConversationService;
import com.example.genz.community.chat.service.MessageSendResult;
import com.example.genz.community.friendship.entity.Friendship;
import com.example.genz.community.friendship.repository.FriendshipRepository;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
@Transactional
class ChatCoreIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ChatMessageService messageService;

    @Autowired
    private ChatReceiptService receiptService;

    @Autowired
    private ChatMessageReceiptRepository receiptRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Test
    void directConversationSupportsIdempotentSendHistoryDeliveryAndRead() {
        User alice = userRepository.saveAndFlush(user("Alice", "0100000001"));
        User bob = userRepository.saveAndFlush(user("Bob", "0100000002"));
        friendshipRepository.saveAndFlush(friendship(alice, bob));

        ConversationResponse conversation = conversationService.createDirectConversation(
                bob.getId(),
                alice.getPhoneNumber()
        );
        UUID clientMessageId = UUID.randomUUID();
        SendMessageRequest request = new SendMessageRequest(clientMessageId, "Hello Bob", null);

        MessageSendResult firstSend = messageService.sendMessage(
                conversation.id(),
                request,
                alice.getPhoneNumber()
        );
        MessageSendResult retry = messageService.sendMessage(
                conversation.id(),
                request,
                alice.getPhoneNumber()
        );

        assertFalse(firstSend.duplicate());
        assertTrue(retry.duplicate());
        assertEquals(firstSend.message().id(), retry.message().id());
        assertEquals(1, receiptRepository.countByConversation_IdAndUser_IdAndReadAtIsNull(
                conversation.id(),
                bob.getId()
        ));

        ChatSliceResponse<MessageResponse> history = messageService.getHistory(
                conversation.id(),
                bob.getPhoneNumber(),
                0,
                20
        );
        assertEquals(1, history.items().size());
        assertEquals("Hello Bob", history.items().getFirst().body());

        receiptService.markDelivered(
                conversation.id(),
                firstSend.message().id(),
                bob.getPhoneNumber()
        );
        receiptService.markRead(
                conversation.id(),
                firstSend.message().id(),
                bob.getPhoneNumber()
        );

        assertEquals(0, receiptRepository.countByConversation_IdAndUser_IdAndReadAtIsNull(
                conversation.id(),
                bob.getId()
        ));
    }

    private User user(String fullName, String phoneNumber) {
        User user = new User();
        user.setFullName(fullName);
        user.setPhoneNumber(phoneNumber);
        user.setDateOfBirth(LocalDate.of(2000, 1, 1));
        user.setGender(Gender.OTHER);
        user.setStatus("ACTIVE");
        return user;
    }

    private Friendship friendship(User first, User second) {
        Friendship friendship = new Friendship();
        if (first.getId().compareTo(second.getId()) < 0) {
            friendship.setUserOne(first);
            friendship.setUserTwo(second);
        } else {
            friendship.setUserOne(second);
            friendship.setUserTwo(first);
        }
        return friendship;
    }
}
