package dev.idachev.recipeservice.config;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.nio.charset.StandardCharsets;

/**
 * Creates and manages the JWT signing key used for token validation.
 */
@Component
@Slf4j
public class JwtKeyConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Getter
    private Key signingKey;

    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            int keyBitSize = keyBytes.length * 8;
            
            // For HS384, need at least 384 bits
            if (keyBitSize < 384) {
                log.warn("JWT secret too small: {} bits < 384 bits - generating secure key", keyBitSize);
                this.signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS384);
            } else {
                this.signingKey = Keys.hmacShaKeyFor(keyBytes);
                log.info("JWT key initialized: {} bits", keyBitSize);
            }
        } catch (Exception e) {
            log.error("JWT key init error: {}", e.getMessage());
            this.signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS384);
        }
    }
} 