package dev.idachev.recipeservice.util;

import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import dev.idachev.recipeservice.config.JwtKeyConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for JWT token operations.
 * Handles token validation and data extraction.
 */
@Component
@Slf4j
public class JwtUtil {

    private final Key signingKey;
    // Fixed service UUID to use consistently for service-to-service authentication
    private static final UUID SERVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public JwtUtil(JwtKeyConfig jwtKeyConfig) {
        this.signingKey = jwtKeyConfig.getSigningKey();
        log.info("JWT validation utility initialized");
    }

    /**
     * Helper method to log token details for debugging
     */
    private void logTokenDetails(String token) {
        try {
            String[] chunks = token.split("\\.");
            if (chunks.length >= 2) {
                Base64.Decoder decoder = Base64.getUrlDecoder();
                String header = new String(decoder.decode(chunks[0]));
                log.debug("JWT token algorithm from header: {}", header.contains("HS384") ? "HS384" : "UNKNOWN");
            }
        } catch (Exception e) {
            log.warn("Error examining token: {}", e.getMessage());
        }
    }

    /**
     * Extract user ID from JWT token.
     */
    public UUID extractUserId(String token) {
        try {
            final Claims claims = extractAllClaims(token);
            // The user-service stores userId as a string, so we should extract it the same
            // way
            String userIdStr = claims.get("userId", String.class);
            if (userIdStr == null || userIdStr.isEmpty()) {
                log.warn("No userId claim found in token or empty value");
                return null;
            }

            try {
                return UUID.fromString(userIdStr);
            } catch (IllegalArgumentException iae) {
                log.error("Invalid UUID format in token: {}", iae.getMessage());
                return null;
            }
        } catch (ExpiredJwtException e) {
            log.error("Token expired while extracting userId: {}", e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            log.error("Malformed token while extracting userId: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error extracting userId from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract username from JWT token.
     */
    public String extractUsername(String token) {
        Claims claims = extractAllClaims(token);

        // Try username claim first, then fall back to subject
        String username = null;

        for (String key : List.of("username", "name", "preferred_username", "email")) {
            if (claims.get(key) != null) {
                username = claims.get(key).toString();
                break;
            }
        }

        // Fall back to subject
        if (username == null) {
            username = claims.getSubject();
        }

        return username != null ? username : "unknown";
    }

    /**
     * Extract authorities/roles from JWT token.
     */
    @SuppressWarnings("unchecked")
    public List<GrantedAuthority> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        List<GrantedAuthority> authorities = new ArrayList<>();

        try {
            // Format 1: "authorities": [{"authority": "ROLE_USER"}, ...]
            if (claims.get("authorities") instanceof List) {
                List<LinkedHashMap<String, String>> authsList = (List<LinkedHashMap<String, String>>) claims
                        .get("authorities");

                if (authsList != null && !authsList.isEmpty()) {
                    authorities = authsList.stream()
                            .filter(map -> map.containsKey("authority"))
                            .map(map -> new SimpleGrantedAuthority(map.get("authority")))
                            .collect(Collectors.toList());
                }
            }

            // Format 2: "roles": ["USER", "ADMIN", ...]
            if (authorities.isEmpty() && claims.get("roles") instanceof List) {
                List<String> roles = (List<String>) claims.get("roles");
                if (roles != null && !roles.isEmpty()) {
                    authorities = roles.stream()
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                }
            }

            // Format 3: "scope" or "scopes" as space-delimited string
            if (authorities.isEmpty()) {
                String scopes = null;
                if (claims.get("scope") instanceof String) {
                    scopes = (String) claims.get("scope");
                } else if (claims.get("scopes") instanceof String) {
                    scopes = (String) claims.get("scopes");
                }

                if (scopes != null && !scopes.isEmpty()) {
                    authorities = Arrays.stream(scopes.split("\\s+"))
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting authorities from token: {}", e.getMessage());
        }

        // Add default role if none found
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return authorities;
    }

    /**
     * Extract all claims from a token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validate JWT token.
     */
    public boolean validateToken(String token) {
        try {
            // Try to decode token header for debugging, without validating signature
            try {
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    Base64.Decoder decoder = Base64.getUrlDecoder();
                    String header = new String(decoder.decode(parts[0]));
                    String payload = new String(decoder.decode(parts[1]));
                    log.debug("JWT token header: {}", header);
                    log.debug("JWT token claims preview: {}",
                            payload.substring(0, Math.min(50, payload.length())) + "...");
                }
            } catch (Exception e) {
                log.warn("Error inspecting token contents: {}", e.getMessage());
            }

            Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);

            log.debug("JWT token validated successfully with recipe-service key");
            return true;
        } catch (ExpiredJwtException e) {
            log.error("JWT token expired: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token format: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature (JWT secret mismatch?): {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate a service token for service-to-service authentication
     * @return JWT token string with service roles
     */
    public String generateServiceToken() {
        try {
            // Create a service token with a fixed service ID
            return Jwts.builder()
                    .setSubject("recipe-service") // Subject is the service name
                    .claim("userId", SERVICE_ID.toString()) // Use consistent service UUID
                    .claim("roles", List.of("ROLE_SERVICE")) // ONLY Service role for authorization
                    .claim("type", "service") // Mark as service token
                    .claim("iss", "recipe-service") // Add issuer claim for additional verification
                    .claim("aud", "user-service") // Add audience claim to specify intended recipient
                    .signWith(signingKey)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour validity
                    .compact();
        } catch (Exception e) {
            log.error("Error generating service token: {}", e.getMessage());
            throw new RuntimeException("Failed to generate service token", e);
        }
    }
}