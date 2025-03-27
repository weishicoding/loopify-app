DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS user_follows;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255),
                       nickname VARCHAR(50) NOT NULL UNIQUE,
                       avatar_url VARCHAR(255),
                       bio TEXT,
                       address VARCHAR(255),
                       is_active BOOLEAN DEFAULT TRUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       last_seen_at TIMESTAMP,  -- Tracks last login/activity
                       INDEX idx_email (email),
                       INDEX idx_nickname (nickname)
);

CREATE TABLE user_follows (
                              follower_id BIGINT NOT NULL,
                              following_id BIGINT NOT NULL,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              PRIMARY KEY (follower_id, following_id),
                              FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
                              FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,
                              INDEX idx_follower (follower_id),
                              INDEX idx_following (following_id)
);


CREATE TABLE refresh_tokens (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                user_id BIGINT NOT NULL,
                                token VARCHAR(255) NOT NULL UNIQUE,
                                expires_at TIMESTAMP NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                is_revoked BOOLEAN DEFAULT FALSE,
                                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                INDEX idx_user_id (user_id),
                                INDEX idx_token (token)
);