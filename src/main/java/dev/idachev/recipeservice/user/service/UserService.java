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
            log.warn("getUsernameById called with null userId");
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        log.info("Attempting to get username for user ID: {}", userId);
        try {
            log.debug("Making API call to user service for user ID: {}", userId);
            ResponseEntity<UserDTO> response = userClient.getUserById(userId);
            
            if (response == null) {
                log.warn("User service returned null response for user ID: {}", userId);
                return "Unknown User";
            }
            
            if (response.getBody() == null) {
                log.warn("User service returned null body for user ID: {}", userId);
                return "Unknown User";
            }
            
            String username = response.getBody().getUsername();
            log.info("Successfully retrieved username '{}' for user ID: {}", username, userId);
            return username;
        } catch (Exception e) {
            log.error("Error retrieving username for user ID {}: {}", userId, e.getMessage(), e);
            return "Unknown User";
        }
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

        try {
            ResponseEntity<UserDTO> response = userClient.getCurrentUser(token);

            if (response.getBody() == null) {
                throw new UnauthorizedException("Invalid authentication token");
            }

            return response.getBody();
        } catch (FeignClientException e) {
            log.error("Error from user-service: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error authenticating user: {}", e.getMessage());
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
            throw new UnauthorizedException("Authorization token is missing or empty");
        }

        if (!token.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid token format: must start with 'Bearer '");
        }
    }

    /**
     * Get user ID from token
     */
    public UUID getUserIdFromToken(String token, String action) {
        validateTokenFormat(token);
        
        try {
            // Make just one API call to get the user profile with ID
            ResponseEntity<UserResponse> userResponse = userClient.getCurrentUserProfile(token);
            if (userResponse.getBody() != null && userResponse.getBody().getId() != null) {
                UUID userId = userResponse.getBody().getId();
                log.debug("User {} (ID: {}) {}", userResponse.getBody().getUsername(), userId, action);
                return userId;
            }
        } catch (Exception e) {
            log.error("Error extracting user ID from token: {}", e.getMessage());
        }
        
        // Only fall back to username-based ID if the profile request fails completely
        try {
            UserDTO user = getCurrentUser(token);
            log.warn("Falling back to deterministic UUID generation for username: {}", user.getUsername());
            return UUID.nameUUIDFromBytes(user.getUsername().getBytes());
        } catch (Exception e) {
            log.error("Complete failure to get user ID, using anonymous ID: {}", e.getMessage());
            return UUID.nameUUIDFromBytes("anonymous".getBytes());
        }
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