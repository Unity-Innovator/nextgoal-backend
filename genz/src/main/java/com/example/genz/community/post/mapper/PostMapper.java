package com.example.genz.community.post.mapper;

import com.example.genz.community.post.dto.PostAuthorResponse;
import com.example.genz.community.post.dto.PostMediaResponse;
import com.example.genz.community.post.dto.PostResponse;
import com.example.genz.community.post.entity.Post;
import com.example.genz.community.post.entity.PostMedia;
import com.example.genz.settings.entity.UserSettings;
import com.example.genz.settings.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostMapper {

    private final UserSettingsRepository settingsRepository;

    public PostResponse toResponse(Post post, UUID currentUserId) {
        boolean owner = post.getAuthor().getId().equals(currentUserId);
        UserSettings settings = owner
                ? null
                : settingsRepository.findByUser_Id(post.getAuthor().getId()).orElse(null);
        String fullName = settings != null && Boolean.FALSE.equals(settings.getShowFullNameOnPosts())
                ? null
                : post.getAuthor().getFullName();
        UUID goalId = post.getGoal() == null
                || (settings != null && Boolean.FALSE.equals(settings.getShowSavingsGoalsPublicly()))
                ? null
                : post.getGoal().getId();
        return new PostResponse(
                post.getId(),
                new PostAuthorResponse(post.getAuthor().getId(), fullName),
                goalId,
                post.getSharedPost() == null ? null : post.getSharedPost().getId(),
                post.getPostType(),
                post.getContent(),
                post.getVisibility(),
                post.getStatus(),
                post.isPinned(),
                owner,
                post.getMedia().stream().map(this::toMediaResponse).toList(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getShareCount(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private PostMediaResponse toMediaResponse(PostMedia media) {
        return new PostMediaResponse(
                media.getId(),
                media.getMediaType(),
                media.getMediaUrl(),
                media.getThumbnailUrl(),
                media.getWidthPx(),
                media.getHeightPx(),
                media.getDurationMs(),
                media.getSortOrder(),
                media.getCreatedAt()
        );
    }
}
