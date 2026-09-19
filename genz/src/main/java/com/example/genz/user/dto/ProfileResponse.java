package com.example.genz.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {
    private ProfileUserResponse user;
    private ProfileLevelResponse level;
    private ProfileSummaryResponse summary;
}
