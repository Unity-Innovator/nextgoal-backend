package com.example.genz.config;

import com.example.genz.security.jwt.JwtUtil;
import com.example.genz.security.user.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthenticationInterceptorTests {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private UserDetails userDetails;

    @Mock
    private MessageChannel channel;

    private WebSocketAuthenticationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthenticationInterceptor(jwtUtil, userDetailsService);
    }

    @Test
    void connectWithValidBearerTokenSetsAuthenticatedPrincipal() {
        when(jwtUtil.extractPhoneNumber("valid-token")).thenReturn("012345678");
        when(userDetailsService.loadUserByUsername("012345678")).thenReturn(userDetails);
        when(jwtUtil.validateToken("valid-token", userDetails)).thenReturn(true);
        when(userDetails.getAuthorities()).thenReturn(List.of());
        when(userDetails.getUsername()).thenReturn("012345678");

        Message<?> message = stompMessage(StompCommand.CONNECT, "Bearer valid-token");
        Message<?> result = interceptor.preSend(message, channel);
        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(
                result,
                StompHeaderAccessor.class
        );

        assertNotNull(resultAccessor);
        assertNotNull(resultAccessor.getUser());
        assertEquals("012345678", resultAccessor.getUser().getName());
    }

    @Test
    void connectWithoutBearerTokenIsRejected() {
        Message<?> message = stompMessage(StompCommand.CONNECT, null);

        assertThrows(
                BadCredentialsException.class,
                () -> interceptor.preSend(message, channel)
        );
    }

    @Test
    void unauthenticatedSendFrameIsRejected() {
        Message<?> message = stompMessage(StompCommand.SEND, null);

        assertThrows(
                BadCredentialsException.class,
                () -> interceptor.preSend(message, channel)
        );
    }

    private Message<?> stompMessage(StompCommand command, String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
