package com.example.genz.community.safety.service;

import com.example.genz.auth.entity.User;
import com.example.genz.community.friendship.repository.FriendshipRepository;
import com.example.genz.community.post.enums.PostVisibility;
import com.example.genz.community.post.exception.PostBadRequestException;
import com.example.genz.settings.entity.UserSettings;
import com.example.genz.settings.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommunityPrivacyService {

    private final UserSettingsRepository userSettingsRepository;
    private final FriendshipRepository friendshipRepository;

    public void requirePostVisibilityAllowed(User author, PostVisibility postVisibility) {
        PostVisibility profileVisibility = profileVisibility(author.getId());
        if (postVisibility.ordinal() < profileVisibility.ordinal()) {
            throw new PostBadRequestException(
                    "Post visibility cannot be broader than your profile visibility"
            );
        }
    }

    public boolean canViewProfile(UUID ownerId, UUID viewerId) {
        if (ownerId.equals(viewerId)) {
            return true;
        }
        PostVisibility visibility = profileVisibility(ownerId);
        if (visibility == PostVisibility.PUBLIC) {
            return true;
        }
        if (visibility == PostVisibility.PRIVATE) {
            return false;
        }
        UUID first = ownerId.compareTo(viewerId) < 0 ? ownerId : viewerId;
        UUID second = ownerId.compareTo(viewerId) < 0 ? viewerId : ownerId;
        return friendshipRepository.existsByUserOne_IdAndUserTwo_Id(first, second);
    }

    public boolean canComment(User postAuthor, UUID actorId) {
        if (postAuthor.getId().equals(actorId)) {
            return true;
        }
        return userSettingsRepository.findByUser_Id(postAuthor.getId())
                .map(UserSettings::getAllowCommentsOnPosts)
                .map(Boolean.TRUE::equals)
                .orElse(true);
    }

    private PostVisibility profileVisibility(UUID userId) {
        return userSettingsRepository.findByUser_Id(userId)
                .map(UserSettings::getProfileVisibility)
                .orElse(PostVisibility.PUBLIC);
    }
}
