package dev.idachev.recipeservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user's favorite recipe.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteRecipe {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID userId;
    
    private UUID recipeId;
    
    private LocalDateTime addedAt;

    @ManyToOne
    @JoinColumn(name = "recipe_id", insertable = false, updatable = false)
    private Recipe recipe;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }
}