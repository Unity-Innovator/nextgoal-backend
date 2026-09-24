package com.example.genz.community.friendship.mapper;

import com.example.genz.auth.entity.User;
import com.example.genz.community.friendship.dto.BlockedUserResponse;
import com.example.genz.community.friendship.dto.FriendRequestResponse;
import com.example.genz.community.friendship.dto.FriendResponse;
import com.example.genz.community.friendship.dto.FriendshipUserResponse;
import com.example.genz.community.friendship.entity.FriendRequest;
import com.example.genz.community.friendship.entity.Friendship;
import com.example.genz.community.friendship.entity.UserBlock;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FriendshipMapper {

    public FriendRequestResponse toFriendRequestResponse(FriendRequest request) {
        return new FriendRequestResponse(
                request.getId(),
                toUserResponse(request.getSender()),
                toUserResponse(request.getReceiver()),
                request.getStatus(),
                request.getCreatedAt(),
                request.getRespondedAt()
        );
    }

    public FriendResponse toFriendResponse(Friendship friendship, UUID currentUserId) {
        User friend = friendship.getUserOne().getId().equals(currentUserId)
                ? friendship.getUserTwo()
                : friendship.getUserOne();
        return new FriendResponse(
                friendship.getId(),
                toUserResponse(friend),
                friendship.getCreatedAt()
        );
    }

    public BlockedUserResponse toBlockedUserResponse(UserBlock block) {
        return new BlockedUserResponse(
                block.getId(),
                toUserResponse(block.getBlockedUser()),
                block.getCreatedAt()
        );
    }

    private FriendshipUserResponse toUserResponse(User user) {
        return new FriendshipUserResponse(user.getId(), user.getFullName());
    }
}
