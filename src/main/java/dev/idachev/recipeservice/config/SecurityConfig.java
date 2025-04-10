package dev.idachev.recipeservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String allowedMethods;

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        log.info("Security configuration initialized with JWT authentication");
    }

    private RequestMatcher apiMatcher() {
        return new OrRequestMatcher(
            new AntPathRequestMatcher("/api/**"),
            new AntPathRequestMatcher("/v1/**")
        );
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        log.info("CORS configuration initialized with allowed origins: {}", allowedOrigins);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain");

        http
                // Enable CORS and disable CSRF for API requests
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                                request, response, 403, "Forbidden", 
                                "You don't have permission to access this resource"))
                        .authenticationEntryPoint((request, response, authException) -> {
                            // Skip auth errors for OPTIONS requests
                            if (request.getMethod().equals("OPTIONS")) {
                                response.setStatus(HttpServletResponse.SC_OK);
                                return;
                            }
                            sendErrorResponse(request, response, 401, "Unauthorized", 
                                "Authentication required");
                        })
                )
                
                // Add JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    private void sendErrorResponse(HttpServletRequest request, HttpServletResponse response, 
                                 int status, String error, String message) throws IOException {
        log.warn("{} for request to {}: {}", error, request.getRequestURI(), message);
        
        if (apiMatcher().matches(request)) {
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            
            String jsonResponse = String.format(
                    "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                    LocalDateTime.now(), status, error, message, request.getRequestURI()
            );
            
            response.getWriter().write(jsonResponse);
        } else {
            response.sendError(status, message);
        }
    }
} 