package dev.idachev.recipeservice.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recipes")
@Slf4j
public class AuthTestController {

    @GetMapping("/auth-test")
    public ResponseEntity<?> testAuth(HttpServletRequest request) {
        log.info("Auth test endpoint called");
        
        // Get the Authorization header
        String authHeader = request.getHeader("Authorization");
        log.info("Authorization header: {}", authHeader);
        
        // Get the current authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", auth != null && auth.isAuthenticated());
        response.put("principal", auth != null ? auth.getPrincipal().toString() : "none");
        response.put("authorities", auth != null ? auth.getAuthorities().toString() : "none");
        
        return ResponseEntity.ok(response);
    }
} 