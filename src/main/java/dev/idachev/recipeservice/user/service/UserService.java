package dev.idachev.recipeservice.user.service;

import dev.idachev.recipeservice.exception.FeignClientException;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedException;
import dev.idachev.recipeservice.user.client.UserClient;
import dev.idachev.recipeservice.user.dto.UserDTO;
import dev.idachev.recipeservice.user.dto.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.UUID;

/**
 * Service for user authentication and identity management
 */
@Service
@Slf4j
public class UserService {

    private final UserClient userClient;

    @Autowired
    public UserService(UserClient userClient) {
        this.userClient = userClient;
    }

    /**
     * Get a username by user ID
     * 
     * @param userId The ID of the user
     * @return The username of the user
     */
    public String getUsernameById(UUID userId) {
        if (userId == null) {
            return "Unknown User";
        }
        
        try {
            // Try the lightweight endpoint first
            ResponseEntity<String> usernameResponse = userClient.getUsernameById(userId);
            if (usernameResponse != null && usernameResponse.getBody() != null) {
                return usernameResponse.getBody();
            }
            
            // Fall back to regular endpoint
            ResponseEntity<UserDTO> response = userClient.getUserById(userId);
            if (response != null && response.getBody() != null) {
                return response.getBody().getUsername();
            }
            
            // Try security context as last resort
            Object principal = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
            if (principal instanceof String) {
                String username = (String) principal;
                if (UUID.nameUUIDFromBytes(username.getBytes()).equals(userId)) {
                    return username;
                }
            }
        } catch (Exception e) {
            log.warn("Error retrieving username for user ID {}: {}", userId, e.getMessage());
        }
        
        return "Unknown User";
    }

    /**
     * Get current user information based on the JWT token.
     *
     * @param token JWT token for authentication
     * @return Current user data
     * @throws UnauthorizedException if token is invalid
     * @throws FeignClientException  if communication with user-service fails
     */
    public UserDTO getCurrentUser(String token) {
        validateTokenFormat(token);
        
        log.debug("Making request to user-service with token: Bearer ***");
        try {
            // Add specific token structure check for debugging
            String tokenValue = token.substring(7);
            String[] parts = tokenValue.split("\\.");
            if (parts.length != 3) {
                log.warn("Token being sent to user-service has invalid structure: parts={}", parts.length);
            }
            
            ResponseEntity<UserDTO> response = userClient.getCurrentUser(token);
            
            if (response == null) {
                log.warn("User service returned null response");
                throw new UnauthorizedException("Invalid authentication response");
            }

            if (response.getBody() == null) {
                log.warn("User service returned null body");
                throw new UnauthorizedException("Invalid authentication token");
            }

            log.debug("Successfully retrieved user information for username: {}", 
                     response.getBody().getUsername());
            return response.getBody();
        } catch (FeignClientException e) {
            log.error("Error from user-service: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error authenticating user: {}", e.getMessage(), e);
            throw new UnauthorizedException("Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Get user by ID.
     *
     * @param userId User ID to retrieve
     * @return User data
     * @throws ResourceNotFoundException if user not found
     * @throws FeignClientException if communication with user-service fails
     */
    public UserDTO getUserById(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        try {
            ResponseEntity<UserDTO> response = userClient.getUserById(userId);

            if (response.getBody() == null) {
                throw new ResourceNotFoundException("User not found with ID: " + userId);
            }

            return response.getBody();
        } catch (FeignClientException e) {
            log.error("Error from user-service: {}", e.getMessage());
            throw e;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving user with ID {}: {}", userId, e.getMessage());
            throw new FeignClientException("Failed to retrieve user: " + e.getMessage());
        }
    }

    /**
     * Validate token format before making API calls
     */
    private void validateTokenFormat(String token) {
        if (!StringUtils.hasText(token)) {
            log.error("Authorization token is missing or empty");
            throw new UnauthorizedException("Authorization token is missing or empty");
        }

        if (!token.startsWith("Bearer ")) {
            log.error("Invalid token format: '{}' does not start with 'Bearer '", 
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);
            throw new UnauthorizedException("Invalid token format: must start with 'Bearer '");
        }
        
        log.debug("Token format validated successfully");
    }

    /**
     * Get user ID from token
     */
    public UUID getUserIdFromToken(String token, String action) {
        validateTokenFormat(token);
        
        // First check if we already have an authenticated user in the security context
        try {
            Object principal = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getPrincipal();
            Object credentials = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getCredentials();
            
            // If credentials is a UUID, it's our user ID
            if (credentials instanceof UUID) {
                UUID userId = (UUID) credentials;
                log.info("Found user ID from security context: {}", userId);
                return userId;
            }
            
            // If principal is a username, we can derive a consistent UUID
            if (principal instanceof String) {
                String username = (String) principal;
                log.info("Found username from security context: {}", username);
                return UUID.nameUUIDFromBytes(username.getBytes());
            }
        } catch (Exception e) {
            log.debug("Could not get user from security context: {}", e.getMessage());
        }
        
        // Next try direct token extraction via JwtUtil
        try {
            String tokenValue = token.substring(7); // Remove "Bearer " prefix
            UUID userId = extractUserIdFromRawToken(tokenValue);
            if (userId != null) {
                log.info("Extracted user ID directly from token: {}", userId);
                return userId;
            }
        } catch (Exception e) {
            log.debug("Could not extract userId directly from token: {}", e.getMessage());
        }
        
        // If direct extraction fails, try the current-user endpoint
        try {
            log.debug("Attempting to get user info from current-user endpoint");
            UserDTO user = getCurrentUser(token);
            if (user != null && StringUtils.hasText(user.getUsername())) {
                String username = user.getUsername();
                log.info("User {} {}", username, action);
                // Since UserDTO doesn't have ID, generate from username - SAME algorithm as user-service
                return UUID.nameUUIDFromBytes(username.getBytes());
            } else {
                log.warn("Retrieved user was null or had empty username");
            }
        } catch (Exception e) {
            log.error("Error authenticating user: {}", e.getMessage(), e);
        }
        
        // If current-user failed, try the profile endpoint as fallback
        try {
            log.debug("Attempting to get user info from profile endpoint");
            ResponseEntity<UserResponse> userResponse = userClient.getCurrentUserProfile(token);
            if (userResponse.getBody() != null && userResponse.getBody().getId() != null) {
                UUID userId = userResponse.getBody().getId();
                log.debug("Fallback - User {} (ID: {}) {}", userResponse.getBody().getUsername(), userId, action);
                return userId;
            } else {
                log.warn("Profile endpoint returned null response or null ID");
            }
        } catch (Exception e) {
            log.error("Error extracting user ID from token: {}", e.getMessage(), e);
        }
        
        // Last resort fallback to anonymous ID - use SAME algorithm as user-service
        String anonymousUser = "anonymous";
        log.error("Complete failure to get user ID, using anonymous ID: {}", 
                  UUID.nameUUIDFromBytes(anonymousUser.getBytes()));
        return UUID.nameUUIDFromBytes(anonymousUser.getBytes());
    }

    /**
     * Helper method to extract user ID directly from a raw JWT token
     */
    private UUID extractUserIdFromRawToken(String token) {
        try {
            // Use Spring utilities to parse and extract from JWT token
            String[] chunks = token.split("\\.");
            if (chunks.length >= 2) {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(chunks[1]));
                
                log.debug("Token payload: {}", payload);
                
                // First check for standard format with quoted userId (most common)
                if (payload.contains("\"userId\"")) {
                    int start = payload.indexOf("\"userId\"") + 9; // "userId":
                    // Find the actual value, accounting for quotes
                    start = payload.indexOf(":", start) + 1;
                    // Skip any whitespace
                    while (start < payload.length() && Character.isWhitespace(payload.charAt(start))) {
                        start++;
                    }
                    
                    boolean isQuoted = start < payload.length() && payload.charAt(start) == '"';
                    if (isQuoted) start++; // Skip the opening quote
                    
                    int end;
                    if (isQuoted) {
                        end = payload.indexOf("\"", start);
                    } else {
                        end = payload.indexOf(",", start);
                        if (end == -1) end = payload.indexOf("}", start);
                    }
                    
                    if (end > start) {
                        String userIdStr = payload.substring(start, end);
                        log.info("Extracted raw userId from token: {}", userIdStr);
                        return UUID.fromString(userIdStr);
                    }
                }
                
                // Fallback approach - try to find sub claim which sometimes contains the ID
                if (payload.contains("\"sub\"")) {
                    int start = payload.indexOf("\"sub\"") + 6; // "sub":
                    start = payload.indexOf(":", start) + 1;
                    while (start < payload.length() && Character.isWhitespace(payload.charAt(start))) {
                        start++;
                    }
                    
                    boolean isQuoted = start < payload.length() && payload.charAt(start) == '"';
                    if (isQuoted) start++; // Skip the opening quote
                    
                    int end;
                    if (isQuoted) {
                        end = payload.indexOf("\"", start);
                    } else {
                        end = payload.indexOf(",", start);
                        if (end == -1) end = payload.indexOf("}", start);
                    }
                    
                    if (end > start) {
                        String subValue = payload.substring(start, end);
                        log.info("Extracted sub from token: {}", subValue);
                        // If it's a UUID, use it directly
                        try {
                            return UUID.fromString(subValue);
                        } catch (IllegalArgumentException e) {
                            // If not a UUID, generate a consistent UUID from the username
                            return UUID.nameUUIDFromBytes(subValue.getBytes());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract userId from token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get user ID from authorization token without action logging
     */
    public UUID getUserIdFromToken(String token) {
        return getUserIdFromToken(token, "requesting resource");
    }

    /**
     * Validate token without returning user information
     */
    public void validateToken(String token) {
        getCurrentUser(token);
    }

    /**
     * Get user ID from username
     */
    public UUID getUserIdFromUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        
        try {
            ResponseEntity<UserResponse> response = userClient.getUserProfileByUsername(username);
            if (response.getBody() != null && response.getBody().getId() != null) {
                return response.getBody().getId();
            }
        } catch (Exception e) {
            log.error("Error retrieving user ID for username {}: {}", username, e.getMessage());
        }
        
        // Fallback to deterministic UUID if user service fails
        log.warn("Falling back to deterministic UUID generation for username: {}", username);
        return UUID.nameUUIDFromBytes(username.getBytes());
    }
} 