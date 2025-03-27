package com.loopify.mainservice.model.user;

import com.loopify.mainservice.model.audit.DateAudit;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_email", columnList = "email"),
                @Index(name = "idx_nickname", columnList = "nickname")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends DateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 255, message = "Email must be less than 255 characters")
    private String email;

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 255, message = "Password must be between 8-255 characters")
    private String password;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Nickname is required")
    @Size(min = 2, max = 50, message = "Nickname must be between 2-50 characters")
    private String nickname;

    @Column(name = "avatar_url")
    @Size(max = 255, message = "Avatar URL must be less than 255 characters")
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Size(max = 255, message = "Address must be less than 255 characters")
    private String address;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_seen_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastSeenAt;

    // Additional methods for business logic
    public boolean isAccountActive() {
        return isActive;
    }

    public void updateLastSeen() {
        this.lastSeenAt = new Date();
    }




}
