package com.example.genz.community.post.mapper;

import com.example.genz.community.post.dto.CommentAuthorResponse;
import com.example.genz.community.post.dto.CommentResponse;
import com.example.genz.community.post.entity.PostComment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CommentMapper {

    public CommentResponse toResponse(
            PostComment comment,
            UUID currentUserId,
            boolean likedByCurrentUser
    ) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParentComment() == null ? null : comment.getParentComment().getId(),
                new CommentAuthorResponse(
                        comment.getAuthor().getId(),
                        comment.getAuthor().getFullName()
                ),
                comment.getContent(),
                comment.getStatus(),
                comment.getLikeCount(),
                comment.getReplyCount(),
                likedByCurrentUser,
                comment.getAuthor().getId().equals(currentUserId),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
