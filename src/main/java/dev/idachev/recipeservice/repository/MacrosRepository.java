package dev.idachev.recipeservice.repository;

import dev.idachev.recipeservice.model.Macros;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for managing Macros entities
 */
@Repository
public interface MacrosRepository extends JpaRepository<Macros, UUID> {
    // Add custom query methods if needed
} 