package com.loopify.mainservice.model.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserFollowsId implements Serializable {

    @Column(name = "follower_id")
    private Long follower;

    @Column(name = "following_id")
    private Long following;
}
