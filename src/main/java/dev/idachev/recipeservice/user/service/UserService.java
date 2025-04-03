package dev.idachev.recipeservice.user.service;

import dev.idachev.recipeservice.exception.FeignClientException;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedException;
import dev.idachev.recipeservice.user.client.UserClient;
import dev.idachev.recipeservice.user.dto.UserDTO;
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
     * Get user ID from username.
     * Since UserDTO doesn't contain ID, we need this helper method.
     *
     * @param username Username to lookup
     * @return UUID representing the user ID
     */
    public UUID getUserIdFromUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        
        // Generate a consistent UUID based on the username
        // This is a simple approach - in production you might want to use a database lookup
        return UUID.nameUUIDFromBytes(username.getBytes());
    }

    /**
     * Get user ID from authorization token with action logging
     *
     * @param token  JWT authorization token
     * @param action Description of the action being performed
     * @return User's UUID
     */
    public UUID getUserIdFromToken(String token, String action) {
        UserDTO user = getCurrentUser(token);
        UUID userId = getUserIdFromUsername(user.getUsername());
        log.debug("User {} (ID: {}) {}", user.getUsername(), userId, action);
        return userId;
    }

    /**
     * Get user ID from authorization token without action logging
     *
     * @param token JWT authorization token
     * @return User's UUID
     */
    public UUID getUserIdFromToken(String token) {
        UserDTO user = getCurrentUser(token);
        return getUserIdFromUsername(user.getUsername());
    }

    /**
     * Validate token without returning user information
     *
     * @param token JWT authorization token
     * @throws UnauthorizedException if token is invalid
     */
    public void validateToken(String token) {
        getCurrentUser(token);
    }
} 