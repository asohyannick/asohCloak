package com.asohCloak.asohCloak.service.userService;

import com.asohCloak.asohCloak.config.asyncScheduler.asyncTaskRunner.AsyncTaskRunner;
import com.asohCloak.asohCloak.config.cacheManagerConfig.twoLevelCacheManager.TwoLevelCacheManager;
import com.asohCloak.asohCloak.config.emailTemplateMessager.EmailTemplateMessager;
import com.asohCloak.asohCloak.config.resendConfig.ResendConfig;
import com.asohCloak.asohCloak.config.securityConfig.SecurityConfig;
import com.asohCloak.asohCloak.mapper.userMappper.UserMapper;
import com.asohCloak.asohCloak.repository.userRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private UserRepository userRepository;
    private SecurityConfig securityConfig;
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    private UserMapper userMapper;
    private EmailTemplateMessager emailTemplateMessager;
    private AsyncTaskRunner asyncTaskRunner;
    private ResendConfig resendConfig;
    private TwoLevelCacheManager twoLevelCacheManager;

}
