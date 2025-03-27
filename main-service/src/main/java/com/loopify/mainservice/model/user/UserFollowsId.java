package com.loopify.mainservice.model.user;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserFollowsId implements Serializable {

    private Long follower;
    private Long following;

    // Default constructor
    public UserFollowsId() {}

    public UserFollowsId(Long followerId, Long followingId) {
        this.follower = followerId;
        this.following = followingId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserFollowsId that = (UserFollowsId) o;
        return Objects.equals(follower, that.follower) &&
                Objects.equals(following, that.following);
    }

    @Override
    public int hashCode() {
        return Objects.hash(follower, following);
    }
}
