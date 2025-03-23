package dev.idachev.recipeservice.user;

import dev.idachev.recipeservice.exception.FeignClientException;
import dev.idachev.recipeservice.exception.UnauthorizedException;
import dev.idachev.recipeservice.user.client.UserClient;
import dev.idachev.recipeservice.user.dto.UserDTO;
import dev.idachev.recipeservice.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceUTest {

    @Mock
    private UserClient userClient;

    @InjectMocks
    private UserService userService;

    @Test
    void givenValidToken_whenGetCurrentUser_thenReturnUserDTO() {

        // Given
        String token = "Bearer valid-jwt-token";
        UserDTO expectedUser = UserDTO.builder()
                .username("testuser")
                .email("test@example.com")
                .build();

        ResponseEntity<UserDTO> responseEntity = ResponseEntity.ok(expectedUser);

        when(userClient.getCurrentUser(token)).thenReturn(responseEntity);

        // When
        UserDTO result = userService.getCurrentUser(token);

        // Then
        assertNotNull(result);
        assertEquals(expectedUser.getUsername(), result.getUsername());
        assertEquals(expectedUser.getEmail(), result.getEmail());

        verify(userClient).getCurrentUser(token);
    }

    @Test
    void givenInvalidToken_whenGetCurrentUser_thenThrowUnauthorizedException() {

        // Given
        String token = "Bearer invalid-token";

        when(userClient.getCurrentUser(token)).thenReturn(ResponseEntity.ok(null));

        // When & Then
        assertThrows(UnauthorizedException.class, () -> userService.getCurrentUser(token));

        verify(userClient).getCurrentUser(token);
    }

    @Test
    void givenFeignClientError_whenGetCurrentUser_thenThrowFeignClientException() {

        // Given
        String token = "Bearer valid-jwt-token";
        FeignClientException expectedException = new FeignClientException("Service unavailable");

        when(userClient.getCurrentUser(token)).thenThrow(expectedException);

        // When & Then
        FeignClientException exception = assertThrows(FeignClientException.class,
                () -> userService.getCurrentUser(token));

        assertEquals(expectedException, exception);
        verify(userClient).getCurrentUser(token);
    }

    @Test
    void givenUsername_whenGetUserIdFromUsername_thenReturnConsistentUUID() {

        // Given
        String username = "testuser";

        // When
        UUID result1 = userService.getUserIdFromUsername(username);
        UUID result2 = userService.getUserIdFromUsername(username);

        // Then
        assertNotNull(result1);
        assertEquals(result1, result2); // Same username should produce same UUID
    }

    @Test
    void givenDifferentUsernames_whenGetUserIdFromUsername_thenReturnDifferentUUIDs() {

        // Given
        String username1 = "user1";
        String username2 = "user2";

        // When
        UUID result1 = userService.getUserIdFromUsername(username1);
        UUID result2 = userService.getUserIdFromUsername(username2);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotEquals(result1, result2); // Different usernames should produce different UUIDs
    }

    @Test
    void givenNullUsername_whenGetUserIdFromUsername_thenThrowNullPointerException() {

        // Given
        String username = null;

        // When & Then
        assertThrows(NullPointerException.class, () -> userService.getUserIdFromUsername(username));
    }

    @Test
    void givenEmptyUsername_whenGetUserIdFromUsername_thenThrowIllegalArgumentException() {

        // Given
        String username = "";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> userService.getUserIdFromUsername(username));
    }

    @Test
    void givenValidToken_whenGetUserIdFromToken_thenReturnUserId() {

        // Given
        String token = "Bearer valid-jwt-token";
        String action = "viewing recipes";

        UserDTO user = UserDTO.builder()
                .username("testuser")
                .build();

        UUID expectedUserId = UUID.nameUUIDFromBytes("testuser".getBytes());

        ResponseEntity<UserDTO> responseEntity = ResponseEntity.ok(user);

        when(userClient.getCurrentUser(token)).thenReturn(responseEntity);

        // When
        UUID result = userService.getUserIdFromToken(token, action);

        // Then
        assertEquals(expectedUserId, result);

        verify(userClient).getCurrentUser(token);
    }
} 