package com.loopify.mainservice.dto.user;

import com.loopify.mainservice.model.user.User;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private String address;
    private Date lastSeenAt;

    public static UserDto fromUser(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .address(user.getAddress())
                .lastSeenAt(user.getLastSeenAt())
                .build();
    }
}
