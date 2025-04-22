package dev.idachev.recipeservice.user.service;

import dev.idachev.recipeservice.exception.FeignClientException;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedException;
import dev.idachev.recipeservice.user.client.UserClient;
import dev.idachev.recipeservice.user.dto.UserDTO;
import dev.idachev.recipeservice.user.dto.UserResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.cache.annotation.Cacheable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Service for interacting with the external User Service.
 * TODO: Review consistency of using UserDTO vs UserResponse. Ensure correct DTO is used based on required detail level.
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
     * Get usernames for a set of user IDs.
     * TODO: Implement corresponding bulk endpoint in User Service & UserClient
     * TODO: Remove placeholder implementation once bulk endpoint is available.
     * 
     * @param userIds Set of user IDs.
     * @return Map of userId to username. Returns empty map until bulk endpoint is implemented.
     */
    public Map<UUID, String> getUsernamesByIds(Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // --- Placeholder Implementation --- 
        // Return empty map until the actual bulk endpoint is available in User Service / UserClient
        log.warn("getUsernamesByIds: Bulk fetching not implemented. Returning empty map. Implement bulk endpoint in User Service!");
        return Collections.emptyMap(); 
        // --- End Placeholder Implementation ---

        /* --- Target Implementation (when UserClient.getUsernamesByIds exists) --- 
        log.debug("Fetching usernames for {} user IDs", userIds.size());
        try {
            ResponseEntity<Map<UUID, String>> response = userClient.getUsernamesByIds(userIds);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched {} usernames", response.getBody().size());
                return response.getBody();
            } else {
                log.error("Failed to fetch usernames for IDs. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                // Return empty map or map with defaults?
                return Collections.emptyMap(); 
            }
        } catch (FeignException e) {
            log.error("Error fetching usernames for IDs {}: {}", userIds, e.getMessage(), e);
            // Return empty map or map with defaults?
            return Collections.emptyMap(); 
        }
        */
    }

    @Cacheable(value = "userNames", key = "#userId")
    public String getUsernameById(UUID userId) {
        if (userId == null) {
             log.warn("Attempted to get username for null userId");
             throw new IllegalArgumentException("User ID cannot be null when fetching username.");
        }
        log.debug("Fetching username for user ID: {}", userId);
        try {
            ResponseEntity<String> response = userClient.getUsernameById(userId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched username for user ID: {}", userId);
                return response.getBody();
            } else {
                log.error("Failed to fetch username for user ID {}. Status: {}, Body: {}", userId, response.getStatusCode(), response.getBody());
                // Consider if 404 should be ResourceNotFound or just return null/empty?
                // Throwing exception aligns with other methods.
                throw new ResourceNotFoundException("Username not found for user ID: " + userId);
            }
        } catch (FeignException e) {
            log.error("Error fetching username for user ID {}: {}", userId, e.getMessage(), e);
            if (e.status() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException("User not found with ID: " + userId, e);
            }
            throw new FeignClientException("User service unavailable while fetching username for ID: " + userId, e);
        }
    }

    // TODO: Review caching strategy for token-based lookups.
    // Caching based solely on the token might be unsafe if tokens are short-lived or easily invalidated.
    // Consider caching based on userId extracted *from* the token (more complex) or using appropriate TTL.
    @Cacheable(value = "currentUser", key = "#token")
    public UserDTO getCurrentUser(String token) {
        // TODO: Implement caching for getCurrentUser
        log.debug("Fetching current user with token");
        try {
            ResponseEntity<UserDTO> response = userClient.getCurrentUser(token);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched current user");
                return response.getBody();
            } else {
                log.error("Failed to fetch current user. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                throw new ResourceNotFoundException("Current user not found.");
            }
        } catch (FeignException e) {
            log.error("Error fetching current user: {}", e.getMessage(), e);
            if (e.status() == HttpStatus.UNAUTHORIZED.value()) {
                throw new UnauthorizedException("Unauthorized: Invalid or expired token.", e);
            }
            // Throw FeignClientException here
            throw new FeignClientException("User service unavailable", e);
        }
    }

    @Cacheable(value = "usersById", key = "#userId")
    public UserDTO getUserById(String token, UUID userId) {
        // TODO: Implement caching for getUserById
        // TODO: Consider if fetching username separately via getUsernameById is needed often
        log.debug("Fetching user by ID: {}", userId);
        try {
            ResponseEntity<UserDTO> response = userClient.getUserById(token, userId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched user by ID: {}", userId);
                return response.getBody();
            } else {
                log.error("Failed to fetch user by ID {}. Status: {}, Body: {}", userId, response.getStatusCode(), response.getBody());
                throw new ResourceNotFoundException("User not found with ID: " + userId);
            }
        } catch (FeignException e) {
            log.error("Error fetching user by ID {}: {}", userId, e.getMessage(), e);
            if (e.status() == HttpStatus.UNAUTHORIZED.value()) {
                throw new UnauthorizedException("Unauthorized access attempt for user ID: " + userId, e);
            }
            if (e.status() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException("User not found with ID: " + userId, e);
            }
            // Throw FeignClientException here
            throw new FeignClientException("User service unavailable", e);
        }
    }

    /**
     * Retrieves the full user profile for the currently authenticated user.
     *
     * @param token The authorization token.
     * @return UserResponse containing the full user profile.
     * @throws UnauthorizedException if the token is invalid or expired.
     * @throws FeignClientException if communication with the user service fails.
     */
    public UserResponse getAuthenticatedUserResponse(String token) {
        log.debug("Fetching authenticated user profile");
        try {
            ResponseEntity<UserResponse> response = userClient.getCurrentUserProfile(token);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched authenticated user profile");
                return response.getBody();
            } else {
                log.error("Failed to fetch authenticated user profile. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                // Throw FeignClientException here for non-2xx success responses
                throw new FeignClientException("Failed to retrieve authenticated user profile. Status: " + response.getStatusCode());
            }
        } catch (FeignException e) {
            log.error("Error fetching authenticated user profile: {}", e.getMessage(), e);
            if (e.status() == HttpStatus.UNAUTHORIZED.value()) {
                throw new UnauthorizedException("Unauthorized: Invalid or expired token.", e);
            }
            // Throw FeignClientException here
            throw new FeignClientException("User service unavailable while fetching profile", e);
        }
    }

    @Cacheable(value = "userResponseByUsername", key = "#username")
    public UserResponse getUserResponseByUsername(String username) {
        // TODO: Add authorization check if necessary? Or assume public profiles?
        // TODO: Implement caching for getUserResponseByUsername
        log.debug("Fetching user profile by username: {}", username);
        try {
            ResponseEntity<UserResponse> response = userClient.getUserProfileByUsername(username);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched user profile for username: {}", username);
                return response.getBody();
            } else {
                log.error("Failed to fetch user profile for username {}. Status: {}, Body: {}", username, response.getStatusCode(), response.getBody());
                throw new ResourceNotFoundException("User profile not found for username: " + username);
            }
        } catch (FeignException e) {
            log.error("Error fetching user profile for username {}: {}", username, e.getMessage(), e);
            if (e.status() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException("User profile not found for username: " + username, e);
            }
            // Throw FeignClientException here
            throw new FeignClientException("User service unavailable while fetching profile for username: " + username, e);
        }
    }

    /**
     * Retrieves the user ID associated with a given username.
     *
     * @param username The username to look up.
     * @return The UUID of the user.
     * @throws ResourceNotFoundException if the username is not found.
     * @throws FeignClientException if communication with the user service fails.
     */
    public UUID getUserIdFromUsername(String username) {
        log.debug("Fetching user ID for username: {}", username);
        try {
            ResponseEntity<UserResponse> response = userClient.getUserProfileByUsername(username);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().getId() != null) {
                log.info("Successfully fetched user ID for username: {}", username);
                return response.getBody().getId();
            } else {
                log.error("Failed to fetch user ID for username {}. Status: {}, Body: {}", username, response.getStatusCode(), response.getBody());
                // If user profile was found but ID is missing, consider this a ResourceNotFound or potentially an internal error.
                // Let's stick with ResourceNotFound for simplicity unless specific error handling is needed.
                throw new ResourceNotFoundException("Could not find user ID for username: " + username);
            }
        } catch (FeignException e) {
            log.error("Error fetching user ID for username {}: {}", username, e.getMessage(), e);
            if (e.status() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException("Username not found: " + username, e);
            }
            // Throw FeignClientException here
            throw new FeignClientException("User service unavailable while fetching user ID for username: " + username, e);
        }
    }

    /**
     * Validate token format before making API calls.
     */
    private void validateTokenFormat(String token) {
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid token format");
        }
    }

    /**
     * Get user ID from token.
     * Primarily relies on fetching the current user profile via API call which returns UserResponse.
     *
     * @param token JWT token (including "Bearer ")
     * @param action Description of action for logging purposes
     * @return The user ID
     * @throws UnauthorizedException if the token is invalid or user cannot be identified
     */
    public UserResponse getUserResponseFromToken(String token, String action) {
        log.debug("Attempting to get user response from token for action: {}", action);
        validateTokenFormat(token);
        try {
            // Primary mechanism: call profile endpoint which returns UserResponse
            log.debug("Requesting current user profile (UserResponse) from user-service...");
            ResponseEntity<UserResponse> response = userClient.getCurrentUserProfile(token);

            UserResponse user = Optional.ofNullable(response)
                .filter(res -> res.getStatusCode().is2xxSuccessful())
                .map(ResponseEntity::getBody)
                .filter(dto -> dto.getId() != null && StringUtils.hasText(dto.getUsername()))
                .orElseThrow(() -> {
                     // Add null check for response and response.getBody() in log message
                     String responseBodyStr = "null body";
                     // Use HttpStatusCode directly
                     org.springframework.http.HttpStatusCode statusCode = null; 
                     if (response != null) {
                         statusCode = response.getStatusCode(); // Assign HttpStatusCode
                         if (response.getBody() != null) {
                             responseBodyStr = response.getBody().toString();
                         }
                     }
                     log.warn("Failed to get valid UserResponse from profile endpoint. Status: {}, Body: {}", 
                              statusCode != null ? statusCode : "null response", 
                              responseBodyStr);
                    return new UnauthorizedException("Invalid token or failed to retrieve user profile.");
                });

            // UUID userId = user.getId(); // No longer needed here
            log.info("User '{}' (ID: {}) performing action: {}", user.getUsername(), user.getId(), action);
            // Return the full UserResponse object
            return user;
        } catch (FeignException e) {
            log.error("FeignException while getting user profile: Status={}, Message={}", e.status(), e.getMessage());
             // Assuming custom UnauthorizedException constructor (String, Throwable)
             throw new UnauthorizedException("Failed to identify user from token: " + e.getMessage(), e);
        } catch (UnauthorizedException e) {
            log.error("UnauthorizedException identifying user from token for action '{}': {}", action, e.getMessage());
             throw e; // Re-throw specific exception
        } catch (Exception e) {
             log.error("Unexpected error identifying user from token for action '{}': {}", action, e.getMessage(), e);
             // Assuming custom UnauthorizedException constructor (String, Throwable)
             throw new UnauthorizedException("Unexpected error identifying user from token.", e);
        }
    }

    // Convenience method without action description
    public UserResponse getUserResponseFromToken(String token) {
        return getUserResponseFromToken(token, "[Unknown Action]");
    }
} 