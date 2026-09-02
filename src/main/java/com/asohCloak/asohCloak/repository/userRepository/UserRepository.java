package com.asohCloak.asohCloak.repository.userRepository;
import com.asohCloak.asohCloak.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByOtpCode(String otpCode);
    Optional<User> findByForgotPassword(String forgotPasswordHash);
    Optional<User> findByMagicLinkToken(String magicLinkTokenHash);
    Page<User> findByAccountDeletedFalse(Pageable pageable);
}
