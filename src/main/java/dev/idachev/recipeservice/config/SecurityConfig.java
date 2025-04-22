package dev.idachev.recipeservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import static org.springframework.security.config.Customizer.withDefaults;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;

import dev.idachev.recipeservice.web.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        log.info("Security configuration initialized with JWT authentication");
    }

    private RequestMatcher apiMatcher() {
        return new OrRequestMatcher(
                new AntPathRequestMatcher("/api/**"),
                new AntPathRequestMatcher("/v1/**")
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain");

        http
                // Enable CORS - Use withDefaults() to apply CORS config from elsewhere (e.g., WebMvcConfigurer)
                .cors(withDefaults())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(apiMatcher())
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))

                // Configure authorization rules
                .authorizeHttpRequests(authorize -> authorize
                        // Always permit OPTIONS requests for CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Allow public endpoints
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/error/**", "/static/**", "/css/**", "/js/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/recipes/auth-test", "/v1/recipes/auth-test").permitAll()

                        // Secured endpoints
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/recipes/generate", "/v1/recipes/generate",
                                "/api/v1/recipes/generate-meal", "/v1/recipes/generate-meal",
                                "/api/v1/recipes/generate-meal-plan", "/v1/recipes/generate-meal-plan",
                                "/api/v1/recipes/generate-recipe", "/v1/recipes/generate-recipe")
                        .hasAnyRole("USER", "ADMIN")

                        // All other requests need authentication
                        .anyRequest().authenticated()
                )

                // Configure stateless session management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure exception handling
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, exception) -> sendErrorResponse(
                                request, response, HttpStatus.FORBIDDEN,
                                "Forbidden", "You don't have permission to access this resource"))
                        .authenticationEntryPoint((request, response, authException) -> {
                            // Skip auth errors for OPTIONS requests
                            if (request.getMethod().equals("OPTIONS")) {
                                response.setStatus(HttpServletResponse.SC_OK);
                                return;
                            }
                            sendErrorResponse(request, response, HttpStatus.UNAUTHORIZED,
                                    "Unauthorized", "Authentication required");
                        })
                )

                // Add JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Sends a standardized JSON error response for API requests, or uses default sendError otherwise.
     */
    private void sendErrorResponse(HttpServletRequest request, HttpServletResponse response,
                                   HttpStatus status, String errorTitle, String detailMessage) throws IOException {
        log.warn("{} error for request to {}: {}", status, request.getRequestURI(), detailMessage);

        if (apiMatcher().matches(request)) {
            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            // Construct message combining the error title and detail
            String combinedMessage = String.format("%s: %s", errorTitle, detailMessage);

            ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                combinedMessage, // Use combined message
                LocalDateTime.now()
                // No details map for these errors
            );
            
            // Use ObjectMapper to write JSON
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        } else {
            // Fallback for non-API requests (e.g., server-rendered pages if any)
            response.sendError(status.value(), detailMessage);
        }
    }
} 