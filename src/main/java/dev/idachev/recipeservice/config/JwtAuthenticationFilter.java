package dev.idachev.recipeservice.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.idachev.recipeservice.util.JwtUtil;
import dev.idachev.recipeservice.web.dto.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher;
    private final ConcurrentHashMap<String, Long> tokenBlacklist;
    private final ObjectMapper objectMapper;
    private final List<String> publicPaths = List.of(
            // API docs and monitoring
            "/api-docs/**", "/swagger-ui/**", "/actuator/**", "/error/**",
            // API test endpoints
            "/api/v1/recipes/auth-test", "/v1/recipes/auth-test"
            // DO NOT skip authentication for api paths - these need proper JWT auth!
    );

    public JwtAuthenticationFilter(JwtUtil jwtUtil, AntPathMatcher pathMatcher,
            ConcurrentHashMap<String, Long> tokenBlacklist,
            ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.pathMatcher = pathMatcher;
        this.tokenBlacklist = tokenBlacklist;
        this.objectMapper = objectMapper;
        log.info("JWT Filter initialized with public path patterns");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Always skip OPTIONS requests for CORS
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        // Check if path matches any of the public patterns
        boolean isPublicPath = publicPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
        
        if (isPublicPath) {
            log.debug("Skipping JWT filter for public path: {}", path);
            return true;
        }
        
        return false;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractJwtFromRequest(request);
            if (StringUtils.hasText(token)) {
                if (isTokenBlacklisted(token)) {
                    handleAuthenticationFailure(response, "Invalid or revoked token", HttpStatus.UNAUTHORIZED);
                    return;
                }

                try {
                    if (jwtUtil.validateToken(token)) {
                        UUID userId = jwtUtil.extractUserId(token);
                        String username = jwtUtil.extractUsername(token);
                        List<GrantedAuthority> authorities = jwtUtil.extractAuthorities(token);

                        // Add detailed logging for troubleshooting user ID issues
                        log.debug("Authentication successful - Username: {}, UserID: {}, Authorities: {}",
                                username, userId, authorities);

                        // Create authentication with userId as the credentials (second parameter)
                        // This allows @AuthenticationPrincipal to correctly extract the UUID
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userId, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        handleAuthenticationFailure(response, "Invalid token", HttpStatus.UNAUTHORIZED);
                        return;
                    }
                } catch (ExpiredJwtException e) {
                    handleAuthenticationFailure(response, "Token expired", HttpStatus.UNAUTHORIZED);
                    return;
                } catch (SignatureException | MalformedJwtException e) {
                    handleAuthenticationFailure(response, "Invalid token format", HttpStatus.UNAUTHORIZED);
                    return;
                } catch (JwtException e) {
                    handleAuthenticationFailure(response, "Token validation failed", HttpStatus.UNAUTHORIZED);
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Auth error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
            log.debug("No valid bearer token found in request headers");
            return "";
        }

        String token = bearerToken.substring(7).trim();
        // Enhanced logging for token length and structure check
        if (!token.isEmpty()) {
            log.debug("Bearer token extracted from request (length: {})", token.length());
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("JWT token has incorrect format - expected 3 parts, got {}", parts.length);
            } else {
                log.debug("JWT token structure valid (header.payload.signature)");
            }
        }
        return token;
    }

    private boolean isTokenBlacklisted(String token) {
        if (!token.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            tokenBlacklist.entrySet().removeIf(entry -> entry.getValue() < currentTime);
            return tokenBlacklist.containsKey(token);
        }
        return false;
    }

    private void handleAuthenticationFailure(HttpServletResponse response,
            String message, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Use ErrorResponse DTO
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                status.getReasonPhrase() + ": " + message, // Combine status reason and specific message
                LocalDateTime.now()
        // No details map
        );

        // Use ObjectMapper to write JSON
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}