package com.asohCloak.asohCloak.service.firebaseAuthService;
import com.asohCloak.asohCloak.exception.badRequestException.BadRequestException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FirebaseAuthService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthService.class);
    private final FirebaseAuth firebaseAuth;

    public FirebaseToken verifyIdToken(String idToken) {
        try {
            return firebaseAuth.verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            log.warn("Firebase ID token verification failed: {}", e.getMessage());
            throw new BadRequestException("Invalid or expired Google sign-in token.");
        }
    }
}