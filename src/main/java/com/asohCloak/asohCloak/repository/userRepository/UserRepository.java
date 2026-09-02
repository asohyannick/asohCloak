package com.asohCloak.asohCloak.repository.userRepository;
import com.asohCloak.asohCloak.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
}
