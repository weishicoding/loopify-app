package com.loopify.mainservice.repository.user;

import com.loopify.mainservice.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    Boolean existsByEmail(String email);
    Boolean existsByNickname(String nickname);
}
