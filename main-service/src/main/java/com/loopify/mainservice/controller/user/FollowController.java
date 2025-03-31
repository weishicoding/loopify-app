package com.loopify.mainservice.controller.user;

import com.loopify.mainservice.dto.user.UserFollowDto;
import com.loopify.mainservice.exception.AppException;
import com.loopify.mainservice.security.CurrentUser;
import com.loopify.mainservice.service.user.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@Slf4j
@RequiredArgsConstructor
public class FollowController {
    private final FollowService followService;

    @PostMapping("/{userId}/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> followUser(@PathVariable Long userId, @CurrentUser Long currentUserId) {
        if (userId.equals(currentUserId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot follow yourself"));
        }
        try {
            boolean result = followService.followUser(currentUserId, userId);

            if (result) {
                return ResponseEntity.ok(Map.of("message", "Successfully followed user"));
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Already following this user"));
            }

        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }

    }

    @DeleteMapping("/{userId}/unfollow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> unfollowUser(@PathVariable Long userId, @CurrentUser Long currentUserId) {
        if (userId.equals(currentUserId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot unfollow yourself"));
        }

        try {
            boolean result = followService.unfollowUser(currentUserId, userId);

            if (result) {
                return ResponseEntity.ok(Map.of("message", "Successfully unfollowed user"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "You were not following this user"));
            }

        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }


    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<Page<UserFollowDto>> getFollowers(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserFollowDto> followers = followService.getFollowers(userId, pageable);
        return ResponseEntity.ok(followers);
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<Page<UserFollowDto>> getFollowing(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserFollowDto> following = followService.getFollowing(userId, pageable);
        return ResponseEntity.ok(following);
    }

    @GetMapping("/{userId}/followers/count")
    public ResponseEntity<Map<String, Long>> getFollowersCount(@PathVariable Long userId) {
        long count = followService.countFollowers(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/{userId}/following/count")
    public ResponseEntity<Map<String, Long>> getFollowingCount(@PathVariable Long userId) {
        long count = followService.countFollowing(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/{userId1}/mutual-followers/{userId2}")
    public ResponseEntity<List<Long>> getMutualFollowers(
            @PathVariable Long userId1,
            @PathVariable Long userId2) {
        List<Long> mutualFollowers = followService.getMutualFollowers(userId1, userId2);
        return ResponseEntity.ok(mutualFollowers);
    }

    @GetMapping("/{userId}/is-following/{targetUserId}")
    public ResponseEntity<Map<String, Boolean>> checkFollowStatus(
            @PathVariable Long userId,
            @PathVariable Long targetUserId) {
        boolean isFollowing = followService.isFollowing(userId, targetUserId);
        return ResponseEntity.ok(Map.of("following", isFollowing));
    }
}
