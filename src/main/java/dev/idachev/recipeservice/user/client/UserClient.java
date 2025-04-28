package dev.idachev.recipeservice.user.client;

import dev.idachev.recipeservice.user.dto.UserDTO;
import dev.idachev.recipeservice.user.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;
import java.util.Set;
import java.util.Map;

/**
 * Client interface for communicating with the user service.
 * Endpoints are split into two categories:
 * 1. Endpoints that require user authentication tokens (marked with @RequestHeader)
 * 2. Endpoints for service-to-service communication that use JWT service tokens via interceptor
 */
@FeignClient(name = "user-service", url = "${app.services.user-service.url}")
public interface UserClient {
    /**
     * Get user information by ID - requires user authentication
     */
    @GetMapping("/api/v1/user/{userId}")
    ResponseEntity<UserDTO> getUserById(@RequestHeader("Authorization") String token, @PathVariable("userId") UUID userId);

    /**
     * Get current user information - requires user authentication
     */
    @GetMapping("/api/v1/user/current-user")
    ResponseEntity<UserDTO> getCurrentUser(@RequestHeader("Authorization") String token);

    /**
     * Get user by ID - service-to-service endpoint
     * Authentication handled by FeignClientConfig interceptor
     */
    @GetMapping("/api/v1/users/{id}")
    ResponseEntity<UserDTO> findUserByIdInternal(@PathVariable("id") UUID userId);
    
    /**
     * Get current user profile - requires user authentication
     */
    @GetMapping("/api/v1/profile")
    ResponseEntity<UserResponse> getCurrentUserProfile(@RequestHeader("Authorization") String token);
    
    /**
     * Get user profile by username - service-to-service endpoint
     * Authentication handled by FeignClientConfig interceptor
     */
    @GetMapping("/api/v1/profile/{username}")
    ResponseEntity<UserResponse> getUserProfileByUsername(@PathVariable("username") String username);

    /**
     * Get username by ID - service-to-service endpoint
     * Authentication handled by FeignClientConfig interceptor
     */
    @GetMapping("/api/v1/users/{id}/username")
    ResponseEntity<String> getUsernameById(@PathVariable("id") UUID userId);

    /**
     * Get usernames for multiple user IDs - service-to-service endpoint
     * Authentication handled by FeignClientConfig interceptor
     * TODO: Ensure this endpoint exists in the User Service
     */
    @GetMapping("/api/v1/users/usernames")
    ResponseEntity<Map<UUID, String>> getUsernamesByIds(@RequestParam("userIds") Set<UUID> userIds);
} 