package com.example.genz.common.exception;


import com.example.genz.auth.exception.OtpException;
import com.example.genz.community.chat.exception.ChatBadRequestException;
import com.example.genz.community.chat.exception.ChatConflictException;
import com.example.genz.community.chat.exception.ChatNotFoundException;
import com.example.genz.community.chat.exception.ChatStorageException;
import com.example.genz.community.friendship.exception.FriendshipBadRequestException;
import com.example.genz.community.friendship.exception.FriendshipConflictException;
import com.example.genz.community.friendship.exception.FriendshipNotFoundException;
import com.example.genz.community.notification.exception.NotificationBadRequestException;
import com.example.genz.community.notification.exception.NotificationNotFoundException;
import com.example.genz.community.moderation.exception.ModerationBadRequestException;
import com.example.genz.community.moderation.exception.ModerationConflictException;
import com.example.genz.community.moderation.exception.ModerationNotFoundException;
import com.example.genz.community.post.exception.PostBadRequestException;
import com.example.genz.community.post.exception.PostNotFoundException;
import com.example.genz.community.safety.exception.CommunityRateLimitExceededException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Order(Ordered.HIGHEST_PRECEDENCE)

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> ValidationHandler(MethodArgumentNotValidException ex){
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ErrorResponse error=new ErrorResponse(message, HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
  
    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ErrorResponse> OtpExceptionHandler(OtpException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }


    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> badRequestHandler(BadRequestException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> authenticationHandler(AuthenticationException ex) {
        ErrorResponse error = new ErrorResponse(
                "Invalid credentials",
                HttpStatus.UNAUTHORIZED.value(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> unauthorizedHandler(UnauthorizedException ex) {
        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED.value(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> AccessDeniedHandler(AccessDeniedException ex){
        ErrorResponse error=new ErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(ChatNotFoundException.class)
    public ResponseEntity<ErrorResponse> chatNotFoundHandler(ChatNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(ChatBadRequestException.class)
    public ResponseEntity<ErrorResponse> chatBadRequestHandler(ChatBadRequestException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(ChatConflictException.class)
    public ResponseEntity<ErrorResponse> chatConflictHandler(ChatConflictException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(ChatStorageException.class)
    public ResponseEntity<ErrorResponse> chatStorageHandler(ChatStorageException ex) {
        log.warn("Chat attachment storage failure", ex);
        ErrorResponse error = new ErrorResponse(
                "Attachment storage is temporarily unavailable",
                HttpStatus.BAD_GATEWAY.value(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(FriendshipNotFoundException.class)
    public ResponseEntity<ErrorResponse> friendshipNotFoundHandler(FriendshipNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(FriendshipBadRequestException.class)
    public ResponseEntity<ErrorResponse> friendshipBadRequestHandler(FriendshipBadRequestException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(FriendshipConflictException.class)
    public ResponseEntity<ErrorResponse> friendshipConflictHandler(FriendshipConflictException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadableMessageHandler(HttpMessageNotReadableException ex) {
        ErrorResponse error = new ErrorResponse(
                "Malformed JSON request or invalid field value",
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> argumentTypeMismatchHandler(MethodArgumentTypeMismatchException ex) {
        ErrorResponse error = new ErrorResponse(
                "Invalid value for parameter: " + ex.getName(),
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponse> postNotFoundHandler(PostNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(PostBadRequestException.class)
    public ResponseEntity<ErrorResponse> postBadRequestHandler(PostBadRequestException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ErrorResponse> notificationNotFoundHandler(NotificationNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(NotificationBadRequestException.class)
    public ResponseEntity<ErrorResponse> notificationBadRequestHandler(NotificationBadRequestException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(CommunityRateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> communityRateLimitHandler(CommunityRateLimitExceededException ex) {
        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.getRetryAfterSeconds()))
                .body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(ModerationNotFoundException.class)
    public ResponseEntity<ErrorResponse> moderationNotFoundHandler(ModerationNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(ModerationBadRequestException.class)
    public ResponseEntity<ErrorResponse> moderationBadRequestHandler(ModerationBadRequestException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(ModerationConflictException.class)
    public ResponseEntity<ErrorResponse> moderationConflictHandler(ModerationConflictException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> ExceptionHandler(Exception ex){
        log.error("Unhandled request failure", ex);
        ErrorResponse error = new ErrorResponse(
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noResourceFoundHandler(NoResourceFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                "Resource not found",
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}

