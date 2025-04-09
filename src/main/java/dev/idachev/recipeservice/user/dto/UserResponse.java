package dev.idachev.recipeservice.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private boolean verified;
    private boolean verificationPending;
    private boolean banned;
    private String role;
    private LocalDateTime createdOn;
    private LocalDateTime lastLogin;
} 