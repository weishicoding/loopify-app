package com.loopify.mainservice.dto.user;

import com.loopify.mainservice.model.user.User;
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

    public static UserFollowDto fromUser(User user, LocalDateTime followedAt, boolean isFollowingBack) {
        return UserFollowDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .followedAt(followedAt)
                .isFollowingBack(isFollowingBack)
                .build();
    }
}
