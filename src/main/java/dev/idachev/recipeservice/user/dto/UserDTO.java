package dev.idachev.recipeservice.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User data transfer object")
public class UserDTO {
    @NotBlank(message = "Username is required")
    @Schema(description = "Username", example = "johndoe", required = true)
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "User email", example = "john.doe@example.com", required = true)
    private String email;
    
    @Schema(description = "Whether the user's account is verified", example = "true")
    private boolean verified;
    
    @Schema(description = "Whether verification is pending for the user's account", example = "false")
    private boolean verificationPending;
    
    @Schema(description = "User's last login date and time", example = "2023-03-15T10:15:30")
    private LocalDateTime lastLogin;
} 