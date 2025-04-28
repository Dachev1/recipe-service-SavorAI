package dev.idachev.recipeservice.user.service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dev.idachev.recipeservice.exception.FeignClientException;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedException;
import dev.idachev.recipeservice.user.client.UserClient;
import dev.idachev.recipeservice.user.dto.UserDTO;
import dev.idachev.recipeservice.user.dto.UserResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for interacting with the external User Service.
 * TODO: Review consistency of using UserDTO vs UserResponse. Ensure correct DTO
 * is used based on required detail level.
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
     * @return Map of userId to username. Returns empty map until bulk endpoint is
     *         implemented.
     */
    public Map<UUID, String> getUsernamesByIds(Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // --- Placeholder Implementation ---
        // Removed - Replaced with actual implementation below
        // --- End Placeholder Implementation ---

        // --- Target Implementation (when UserClient.getUsernamesByIds exists) ---
        log.debug("Fetching usernames for {} user IDs", userIds.size());
        try {
            ResponseEntity<Map<UUID, String>> response = userClient.getUsernamesByIds(userIds);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched {} usernames", response.getBody().size());
                return response.getBody();
            } else {
                log.error("Failed to fetch usernames for IDs. Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                // Return empty map on failure to prevent breaking the entire request flow.
                return Collections.emptyMap();
            }
        } catch (FeignException e) {
            log.error("Error fetching usernames for IDs {}: {}", userIds, e.getMessage(),
                    e);
            // Return empty map on failure.
            return Collections.emptyMap();
        }
        // --- End Target Implementation ---
    }

    @Cacheable(value = "userNames", key = "#userId")
    public String getUsernameById(UUID userId) {
        if (userId == null) {
            log.warn("getUsernameById called with null userId."); // Add logging
            return "Unknown User";
        }
        
        log.debug("Fetching username for user ID: {}", userId); // Add logging
        try {
            ResponseEntity<String> response = userClient.getUsernameById(userId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched username '{}' for ID: {}", response.getBody(), userId); // Add logging
                return response.getBody();
            } else {
                 // Log non-successful responses
                 log.warn("Failed to fetch username for ID {}. Status: {}, Body: {}", userId, response.getStatusCode(), response.getBody());
                 return "Unknown User";
            }
        } catch (FeignException e) { // Catch specific Feign exceptions
            // Log Feign client errors more specifically
             log.error("FeignClient error fetching username for ID {}: Status={}, Message={}", userId, e.status(), e.getMessage());
             // Optionally check e.status() for specific handling (e.g., 404 Not Found)
             if (e.status() == 404) {
                 log.warn("User ID {} not found in user-service.", userId);
             }
             return "Unknown User";
        } catch (Exception e) { // Catch any other unexpected exceptions
             log.error("Unexpected error fetching username for ID {}: {}", userId, e.getMessage(), e);
             return "Unknown User";
        }
        
        // This line should theoretically not be reached if the try block returns or throws
        // return "Unknown User"; // Removed redundant return
    }

    // Consider caching based on userId extracted *from* the token (more complex) or
    // using appropriate TTL.
    // @Cacheable(value = "currentUser", key = "#token") // REMOVED - Caching by token is ineffective/unsafe
    public UserDTO getCurrentUser(String token) {
        // TODO: Implement caching for getCurrentUser // REMOVED - @Cacheable was removed
        log.debug("Fetching current user with token");
        try {
            ResponseEntity<UserDTO> response = userClient.getCurrentUser(token);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched current user");
                return response.getBody();
            } else {
                log.error("Failed to fetch current user. Status: {}, Body: {}", response.getStatusCode(),
                        response.getBody());
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
        // TODO: Implement caching for getUserById // REMOVED - @Cacheable is present
        // TODO: Consider if fetching username separately via getUsernameById is needed often
        log.debug("Fetching user by ID: {}", userId);
        try {
            ResponseEntity<UserDTO> response = userClient.getUserById(token, userId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched user by ID: {}", userId);
                return response.getBody();
            } else {
                log.error("Failed to fetch user by ID {}. Status: {}, Body: {}", userId, response.getStatusCode(),
                        response.getBody());
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
     * @throws FeignClientException  if communication with the user service fails.
     */
    public UserResponse getAuthenticatedUserResponse(String token) {
        log.debug("Fetching authenticated user profile");
        try {
            ResponseEntity<UserResponse> response = userClient.getCurrentUserProfile(token);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched authenticated user profile");
                return response.getBody();
            } else {
                log.error("Failed to fetch authenticated user profile. Status: {}, Body: {}", response.getStatusCode(),
                        response.getBody());
                // Throw FeignClientException here for non-2xx success responses
                throw new FeignClientException(
                        "Failed to retrieve authenticated user profile. Status: " + response.getStatusCode());
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
        // TODO: Implement caching for getUserResponseByUsername // REMOVED - @Cacheable is present
        log.debug("Fetching user profile by username: {}", username);
        try {
            ResponseEntity<UserResponse> response = userClient.getUserProfileByUsername(username);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched user profile for username: {}", username);
                return response.getBody();
            } else {
                log.error("Failed to fetch user profile for username {}. Status: {}, Body: {}", username,
                        response.getStatusCode(), response.getBody());
                throw new ResourceNotFoundException("User profile not found for username: " + username);
            }
        } catch (FeignException e) {
            log.error("Error fetching user profile for username {}: {}", username, e.getMessage(), e);
            if (e.status() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException("User profile not found for username: " + username, e);
            }
            // Throw FeignClientException here
            throw new FeignClientException("User service unavailable while fetching profile for username: " + username,
                    e);
        }
    }

    /**
     * Retrieves the user ID associated with a given username.
     *
     * @param username The username to look up.
     * @return The UUID of the user.
     * @throws ResourceNotFoundException if the username is not found.
     * @throws FeignClientException      if communication with the user service
     *                                   fails.
     */
    public UUID getUserIdFromUsername(String username) {
        log.debug("Fetching user ID for username: {}", username);
        try {
            ResponseEntity<UserResponse> response = userClient.getUserProfileByUsername(username);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null
                    && response.getBody().getId() != null) {
                log.info("Successfully fetched user ID for username: {}", username);
                return response.getBody().getId();
            } else {
                log.error("Failed to fetch user ID for username {}. Status: {}, Body: {}", username,
                        response.getStatusCode(), response.getBody());
                // If user profile was found but ID is missing, consider this a ResourceNotFound
                // or potentially an internal error.
                // Let's stick with ResourceNotFound for simplicity unless specific error
                // handling is needed.
                throw new ResourceNotFoundException("Could not find user ID for username: " + username);
            }
        } catch (FeignException e) {
            log.error("Error fetching user ID for username {}: {}", username, e.getMessage(), e);
            if (e.status() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException("Username not found: " + username, e);
            }
            // Throw FeignClientException here
            throw new FeignClientException("User service unavailable while fetching user ID for username: " + username,
                    e);
        }
    }

    private void validateTokenFormat(String token) {
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid token format");
        }
    }

    /**
     * Gets user information from the token by calling the user profile endpoint.
     * 
     * @param token  The JWT token (including "Bearer " prefix)
     * @param action Description of the action being performed (used for logging)
     * @return UserResponse with user details
     * @throws UnauthorizedException if token is invalid or missing prefix
     * @throws FeignClientException  if communication with user service fails
     */
    public UserResponse getUserResponseFromToken(String token, String action) {
        validateTokenFormat(token);
        log.debug("Fetching user profile using token for action: {}", action); // Add logging
        // Reuse the method that correctly calls the user profile endpoint
        try {
            return getAuthenticatedUserResponse(token);
        } catch (UnauthorizedException | FeignClientException e) {
            // Log the specific action that failed
            log.error("Failed to get user profile for action \"{}\": {}", action, e.getMessage());
            throw e; // Re-throw the original exception
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            log.error("Unexpected error getting user profile for action \"{}\": {}", action, e.getMessage(), e);
            throw new FeignClientException("Unexpected error validating token for action: " + action, e);
        }
    }

    // Convenience method without action description
    public UserResponse getUserResponseFromToken(String token) {
        return getUserResponseFromToken(token, "[Unknown Action]");
    }
}