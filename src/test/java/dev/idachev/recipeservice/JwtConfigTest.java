package dev.idachev.recipeservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class JwtConfigTest {

    @Value("${JWT_SECRET}")
    private String jwtSecret;
    
    @Test
    public void testJwtSecretLoaded() {
        assertNotNull(jwtSecret);
        // The actual value in your .env.properties
        assertEquals("cHxkdyB6ZnlqIHFjaHYgeGJ3cABwW5zaGdqaGQK2j1mn2j3bas9", jwtSecret);
    }
} 