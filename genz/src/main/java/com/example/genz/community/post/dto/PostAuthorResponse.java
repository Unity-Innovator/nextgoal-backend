package com.example.genz.community.post.dto;

import java.util.UUID;

public record PostAuthorResponse(
        UUID id,
        String fullName
) {
}
