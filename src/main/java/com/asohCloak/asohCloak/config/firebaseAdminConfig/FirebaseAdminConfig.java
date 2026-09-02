package com.asohCloak.asohCloak.config.firebaseAdminConfig;
import com.asohCloak.asohCloak.config.firebaseAdminConfig.firebaseProperties.FirebaseProperties;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import java.io.IOException;
import java.util.List;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
@RequiredArgsConstructor
public class FirebaseAdminConfig {

    private final FirebaseProperties firebaseProperties;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        String privateKeyPkcs8 = firebaseProperties.getPrivateKey().replace("\\n", "\n");

        GoogleCredentials credentials = ServiceAccountCredentials.fromPkcs8(
                firebaseProperties.getClientId(),
                firebaseProperties.getClientEmail(),
                privateKeyPkcs8,
                firebaseProperties.getPrivateKeyId(),
                List.of("https://www.googleapis.com/auth/identitytoolkit")
        );

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(firebaseProperties.getProjectId())
                .build();

        return FirebaseApp.getApps().isEmpty()
                ? FirebaseApp.initializeApp(options)
                : FirebaseApp.getInstance();
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}