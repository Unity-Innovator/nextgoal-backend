package com.example.genz.community.post.service;

import com.example.genz.community.friendship.repository.FriendshipRepository;
import com.example.genz.community.friendship.repository.UserBlockRepository;
import com.example.genz.community.post.entity.Post;
import com.example.genz.community.post.enums.PostVisibility;
import com.example.genz.community.safety.service.CommunityPrivacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostAccessService {

    private final FriendshipRepository friendshipRepository;
    private final UserBlockRepository userBlockRepository;
    private final CommunityPrivacyService privacyService;

    public boolean canView(Post post, UUID viewerId) {
        UUID authorId = post.getAuthor().getId();
        if (authorId.equals(viewerId)) {
            return true;
        }
        if (isBlockedEitherWay(authorId, viewerId)) {
            return false;
        }
        if (!privacyService.canViewProfile(authorId, viewerId)) {
            return false;
        }
        if (post.getVisibility() == PostVisibility.PUBLIC) {
            return true;
        }
        if (post.getVisibility() == PostVisibility.PRIVATE) {
            return false;
        }
        IdPair pair = orderedIds(authorId, viewerId);
        return friendshipRepository.existsByUserOne_IdAndUserTwo_Id(pair.first(), pair.second());
    }

    private boolean isBlockedEitherWay(UUID firstUserId, UUID secondUserId) {
        return userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(firstUserId, secondUserId)
                || userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(secondUserId, firstUserId);
    }

    private IdPair orderedIds(UUID first, UUID second) {
        return first.compareTo(second) < 0
                ? new IdPair(first, second)
                : new IdPair(second, first);
    }

    private record IdPair(UUID first, UUID second) {
    }
}
