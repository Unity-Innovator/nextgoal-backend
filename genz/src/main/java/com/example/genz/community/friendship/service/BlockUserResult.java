package com.example.genz.community.friendship.service;

import com.example.genz.community.friendship.dto.BlockedUserResponse;

public record BlockUserResult(
        BlockedUserResponse block,
        boolean created
) {
}
