package com.loopify.mainservice.repository.user;

import com.loopify.mainservice.model.user.UserFollows;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserFollowsRepository extends JpaRepository<UserFollows, Long> {

    @Query(value = "SELECT uf FROM UserFollows uf WHERE uf.follower.id = :userId")
    List<UserFollows> findAllFollowingByUserId(@Param("userId") Long userId);

    @Query("SELECT uf FROM UserFollows uf WHERE uf.following.id = :userId")
    List<UserFollows> findAllFollowersByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(uf) FROM UserFollows uf WHERE uf.follower.id = :userId")
    long countFollowing(@Param("userId") Long userId);

    @Query("SELECT COUNT(uf) FROM UserFollows uf WHERE uf.following.id = :userId")
    long countFollowers(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(uf) > 0 THEN true ELSE false END FROM UserFollows uf " +
            "WHERE uf.follower.id = :followerId AND uf.following.id = :followingId")
    boolean existsByFollowerIdAndFollowingId(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Query("DELETE FROM UserFollows uf WHERE uf.follower.id = :followerId AND uf.following.id = :followingId")
    @Modifying
    void deleteByFollowerIdAndFollowingId(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
}
