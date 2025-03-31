package com.loopify.mainservice.model.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_follows")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFollows {

    @EmbeddedId
    private UserFollowsId id;

    @ManyToOne
    @JoinColumn(name = "follower_id", nullable = false, insertable = false, updatable = false)
    private User follower;

    @ManyToOne
    @JoinColumn(name = "following_id", nullable = false, insertable = false, updatable = false)
    private User following;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructor for convenience
    public UserFollows(User follower, User following) {
        this.follower = follower;
        this.following = following;
        this.id = new UserFollowsId(follower.getId(), following.getId());
    }
}
