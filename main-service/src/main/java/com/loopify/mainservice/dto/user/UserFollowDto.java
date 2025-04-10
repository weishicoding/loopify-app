package com.loopify.mainservice.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFollowDto {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private LocalDateTime followedAt;
    private boolean isFollowingBack;
}
