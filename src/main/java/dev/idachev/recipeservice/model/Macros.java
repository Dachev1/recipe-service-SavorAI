package dev.idachev.recipeservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Macros entity representing nutritional information for a recipe.
 */
@Entity
@Table(name = "macros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Macros {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Column()
    private Integer calories;

    @Column()
    private Double proteinGrams;

    @Column()
    private Double carbsGrams;

    @Column()
    private Double fatGrams;
} 