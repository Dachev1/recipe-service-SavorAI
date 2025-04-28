package dev.idachev.recipeservice.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Web MVC configuration including CORS settings.
 * Uses type-safe CorsProperties.
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    @Autowired // Inject properties bean
    public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        // Use properties directly from the bean
        registry.addMapping("/**")
                .allowedOrigins(corsProperties.allowedOrigins().toArray(new String[0])) // Convert List to String[]
                .allowedMethods(corsProperties.allowedMethods().toArray(new String[0]))
                .allowedHeaders(corsProperties.allowedHeaders().toArray(new String[0]))
                .allowCredentials(corsProperties.allowCredentials())
                .maxAge(3600); // maxAge could also be configurable
    }
    
    /**
     * Configure special handling for frontend-style routes
     * This configures routes that should be handled by controllers
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // No longer returning 204 for /v1/** routes as they should be handled by controllers
        log.info("CORS configuration applied - allowing origins: {}", corsProperties.allowedOrigins());
    }
    
    /**
     * Add support for correctly resolving the UUID as authentication principal
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new UUIDAuthenticationPrincipalResolver());
        log.info("Added custom UUID AuthenticationPrincipal resolver");
    }
    
    /**
     * Custom resolver for @AuthenticationPrincipal that handles UUIDs correctly
     */
    private static class UUIDAuthenticationPrincipalResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class) && 
                   parameter.getParameterType().equals(UUID.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return null;
            }
            
            Object principal = authentication.getPrincipal();
            if (principal instanceof UUID) {
                return principal;
            }
            
            return null;
        }
    }
} 