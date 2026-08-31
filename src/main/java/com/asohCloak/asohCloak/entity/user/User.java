package com.asohCloak.asohCloak.entity.user;
import com.asohCloak.asohCloak.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.util.UUID;
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_email", columnList = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role = UserRole.BURSAR;

    @Column(nullable = false)
    private boolean accountVerified = false;

    @Column(nullable = false)
    private boolean accountBlocked = false;

    @Column(nullable = false)
    private boolean accountDeleted = false;

    @Column(nullable = false)
    private boolean accountSuspended = false;

    @Column(nullable = false)
    private boolean accountLocked = false;

    @Column(length = 20)
    private String otpCode;

    @Column(nullable = false)
    private boolean otpCodeVerified = false;

    private Instant otpExpiryDate;

    @Column
    private String magicLinkToken;

    @Column
    private String forgotPassword;

    @Column
    private String resetPassword;

    @Column(nullable = true)
    private  int failedLoginAttempts;

    @JsonIgnore
    private Instant lockedUntil;

    @Column(nullable = true)
    private String verifyMagicLinkToken;

    @Column(nullable = false)
    private Instant magicLinkExpiryDate;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column
    private String accessToken;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column
    private String refreshToken;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    private void normalize() {
        if (email != null) {
            email = email.toLowerCase();
        }
    }
}
