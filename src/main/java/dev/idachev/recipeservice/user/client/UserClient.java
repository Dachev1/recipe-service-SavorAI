package dev.idachev.recipeservice.user.client;

import dev.idachev.recipeservice.user.dto.UserDTO;
import dev.idachev.recipeservice.user.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

/**
 * Client interface for communicating with the user service.
 * Note: All token parameters must include the "Bearer " prefix.
 */
@FeignClient(name = "user-service", url = "${app.services.user-service.url}")
public interface UserClient {
    /**
     * Get user information by ID.
     */
    @GetMapping("/api/v1/user/{userId}")
    ResponseEntity<UserDTO> getUserById(@RequestHeader("Authorization") String token, @PathVariable("userId") UUID userId);

    /**
     * Get current user information (basic version).
     */
    @GetMapping("/api/v1/user/current-user")
    ResponseEntity<UserDTO> getCurrentUser(@RequestHeader("Authorization") String token);

    /**
     * Get user by ID without authentication - internal service communication.
     */
    @GetMapping("/api/v1/users/{id}")
    ResponseEntity<UserDTO> getUserById(@PathVariable("id") UUID userId);
    
    /**
     * Get current user profile with complete information.
     */
    @GetMapping("/api/v1/profile")
    ResponseEntity<UserResponse> getCurrentUserProfile(@RequestHeader("Authorization") String token);
    
    /**
     * Get user profile by username.
     */
    @GetMapping("/api/v1/profile/{username}")
    ResponseEntity<UserResponse> getUserProfileByUsername(@PathVariable("username") String username);

    /**
     * Get username by ID - lightweight endpoint for internal use.
     */
    @GetMapping("/api/v1/users/{id}/username")
    ResponseEntity<String> getUsernameById(@PathVariable("id") UUID userId);
} 