package com.loopify.mainservice.service.user;

import com.loopify.mainservice.dto.user.UserFollowDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FollowService {

    boolean followUser(Long followerId, Long followingId);

    boolean unfollowUser(Long followerId, Long followingId);

    Page<UserFollowDto> getFollowers(Long userId, Pageable pageable);

    Page<UserFollowDto> getFollowing(Long userId, Pageable pageable);

    long countFollowers(Long userId);

    long countFollowing(Long userId);

    boolean isFollowing(Long followerId, Long followingId);

    List<Long> getMutualFollowers(Long userId1, Long userId2);
}
