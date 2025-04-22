package dev.idachev.recipeservice.config;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * Creates and manages the JWT signing key used for token validation.
 * Uses type-safe JwtProperties.
 */
@Component
@Slf4j
public class JwtKeyConfig {

    private final JwtProperties jwtProperties;

    @Getter
    private Key signingKey;

    @Autowired
    public JwtKeyConfig(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        try {
            String secret = jwtProperties.secret();
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            int keyBitSize = keyBytes.length * 8;

            // For HS384, need at least 384 bits
            if (keyBitSize < 384) {
                log.warn("JWT secret too small despite config validation check: {} bits < 384 bits - generating secure key", keyBitSize);
                this.signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS384);
            } else {
                this.signingKey = Keys.hmacShaKeyFor(keyBytes);
                log.info("JWT key initialized from configured secret: {} bits", keyBitSize);
            }
        } catch (Exception e) {
            log.error("JWT key initialization error: {}. Falling back to generated key.", e.getMessage());
            this.signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS384);
        }
    }
} 