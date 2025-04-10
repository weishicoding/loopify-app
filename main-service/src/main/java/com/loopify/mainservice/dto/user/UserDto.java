package com.loopify.mainservice.dto.user;

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
}
