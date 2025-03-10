package dev.idachev.recipeservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.UUID;

/**
 * Utility class for JWT token operations.
 */
@Component
public class JwtUtil {

    private SecretKey key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        try {
            // Try to use the provided secret
            this.key = Keys.hmacShaKeyFor(secret.getBytes());
        } catch (io.jsonwebtoken.security.WeakKeyException e) {
            // If the key is too weak, generate a secure key for HS256
            System.out.println("Warning: Provided JWT secret is too weak. Generating a secure key instead.");
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
    }

    /**
     * Extract user ID from JWT token.
     *
     * @param token the JWT token
     * @return the user ID
     */
    public UUID extractUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Validate JWT token.
     *
     * @param token the JWT token
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 