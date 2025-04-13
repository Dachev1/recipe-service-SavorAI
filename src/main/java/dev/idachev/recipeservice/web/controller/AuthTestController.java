package dev.idachev.recipeservice.web.controller;

import dev.idachev.recipeservice.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/auth-test", "/v1/auth-test"})
@Tag(name = "Auth Test", description = "Debug endpoints for authentication testing")
@Slf4j
public class AuthTestController {

    private final UserService userService;

    public AuthTestController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Test authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> testAuth(@RequestHeader("Authorization") String token) {
        UUID userId = userService.getUserIdFromToken(token);
        
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("userId", userId);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Debug token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token debug info"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/token-debug")
    public ResponseEntity<Map<String, Object>> debugToken(@RequestHeader("Authorization") String token) {
        UUID userId = userService.getUserIdFromToken(token);
        
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("userId", userId);
        
        // Parse token for debugging
        try {
            String actualToken = token.substring(7); // Remove Bearer prefix
            String[] chunks = actualToken.split("\\.");
            
            if (chunks.length >= 2) {
                Base64.Decoder decoder = Base64.getUrlDecoder();
                
                String header = new String(decoder.decode(chunks[0]));
                String payload = new String(decoder.decode(chunks[1]));
                
                // Don't include the actual token or its contents, only safe info
                response.put("tokenInfo", Map.of(
                    "format", "JWT",
                    "containsUserId", payload.contains("userId"),
                    "containsUsername", payload.contains("username") || payload.contains("preferred_username"),
                    "containsSub", payload.contains("sub")
                ));
            }
        } catch (Exception e) {
            response.put("tokenParseError", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
} 