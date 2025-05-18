package com.loopify.mainservice.service.user.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopify.mainservice.dto.user.UserFollowDto;
import com.loopify.mainservice.enums.NotificationType;
import com.loopify.mainservice.exception.AppException;
import com.loopify.mainservice.model.NotificationOutbox;
import com.loopify.mainservice.model.user.User;
import com.loopify.mainservice.model.user.UserFollows;
import com.loopify.mainservice.notification.BaseNotification;
import com.loopify.mainservice.notification.FollowNotification;
import com.loopify.mainservice.repository.notification.NotificationOutboxRepository;
import com.loopify.mainservice.repository.user.UserFollowsRepository;
import com.loopify.mainservice.repository.user.UserRepository;
import com.loopify.mainservice.service.user.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowServiceImpl implements FollowService {

    private final UserFollowsRepository userFollowsRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final NotificationOutboxRepository notificationOutboxRepository;

    @Override
    @Transactional
    public boolean followUser(Long followerId, Long followingId) throws JsonProcessingException {
        // Can't follow yourself
        if (followerId.equals(followingId)) {
            log.warn("User {} attempted to follow themselves", followerId);
            return false;
        }

        // Check if already following
        if (userFollowsRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            log.info("User {} is already following user {}", followerId, followingId);
            return false;
        }

        // Get user entities
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new AppException("User not found with id: " + followerId));

        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new AppException("User not found with id: " + followingId));

        // Create follow relationship
        UserFollows userFollows = UserFollows.builder()
                .follower(follower)
                .following(following)
                .createdAt(LocalDateTime.now())
                .build();

        userFollowsRepository.save(userFollows);




        Long notificationId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        BaseNotification notification = new FollowNotification(
                notificationId, // notificationId
                following.getId(),
                follower.getId(),
                userFollows.getFollower().getNickname(),
                userFollows.getFollower().getAvatarUrl()
        );

        // persist data to notification outbox
        NotificationOutbox notificationOutbox = NotificationOutbox.builder()
                .id(notificationId)
                .notificationType(NotificationType.FOLLOW)
                .payload(objectMapper.writeValueAsString(notification))
                .build();

        notificationOutboxRepository.save(notificationOutbox);
        log.info("User {} started following user {}", followerId, followingId);

        return true;
    }

    @Override
    @Transactional
    public boolean unfollowUser(Long followerId, Long followingId) {

        if (!userFollowsRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            log.info("User {} was not following user {}", followerId, followingId);
            return false;
        }

        userFollowsRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
        log.info("User {} unfollowed user {}", followerId, followingId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserFollowDto> getFollowers(Long userId, Pageable pageable) {
        List<UserFollows> followers = userFollowsRepository.findAllFollowersByUserId(userId);
        long totalFollowers = userFollowsRepository.countFollowers(userId);

        List<UserFollowDto> followerDtos = followers.stream()
                .map(follow -> {
                    User follower = follow.getFollower();
                    boolean isFollowingBack = userFollowsRepository.existsByFollowerIdAndFollowingId(userId, follower.getId());
                    return UserFollowDto.builder()
                            .userId(follower.getId())
                            .nickname(follower.getNickname())
                            .avatarUrl(follower.getAvatarUrl())
                            .followedAt(follow.getCreatedAt())
                            .isFollowingBack(isFollowingBack)
                            .build();
                })
                .collect(Collectors.toList());

        return new PageImpl<>(followerDtos, pageable, totalFollowers);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserFollowDto> getFollowing(Long userId, Pageable pageable) {
        List<UserFollows> followings = userFollowsRepository.findAllFollowingByUserId(userId);
        long totalFollowings = userFollowsRepository.countFollowing(userId);

        List<UserFollowDto> followingDtos = followings.stream()
                .map(follow -> {
                    User following = follow.getFollowing();
                    boolean isFollowingBack = userFollowsRepository.existsByFollowerIdAndFollowingId(
                            following.getId(), userId
                    );
                    return UserFollowDto.builder()
                            .userId(following.getId())
                            .nickname(following.getNickname())
                            .avatarUrl(following.getAvatarUrl())
                            .followedAt(follow.getCreatedAt())
                            .isFollowingBack(isFollowingBack)
                            .build();
                })
                .collect(Collectors.toList());

        return new PageImpl<>(followingDtos, pageable, totalFollowings);
    }

    @Override
    @Transactional(readOnly = true)
    public long countFollowers(Long userId) {
        return userFollowsRepository.countFollowers(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countFollowing(Long userId) {
        return userFollowsRepository.countFollowing(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followingId) {
        return userFollowsRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getMutualFollowers(Long userId1, Long userId2) {
        List<UserFollows> followersUser1 = userFollowsRepository.findAllFollowersByUserId(userId1);

        List<UserFollows> followersUser2 = userFollowsRepository.findAllFollowersByUserId(userId2);

        // Extract follower IDs
        List<Long> followerIdsUser1 = followersUser1.stream()
                .map(uf -> uf.getFollower().getId())
                .toList();

        // Find common followers
        return followersUser2.stream()
                .map(uf -> uf.getFollower().getId())
                .filter(followerIdsUser1::contains)
                .collect(Collectors.toList());
    }
}
