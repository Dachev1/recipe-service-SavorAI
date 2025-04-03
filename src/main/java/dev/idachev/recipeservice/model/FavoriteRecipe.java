package dev.idachev.recipeservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "favorite_recipes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteRecipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private UUID userId;
    
    @Column(nullable = false)
    private UUID recipeId;
    
    private LocalDateTime createdAt;
}