package dev.idachev.recipeservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column
    private String title;

    @Column
    private String description;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String ingredients;

    @Column
    private UUID userId;

    @Column
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column
    private Integer totalTimeMinutes;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Macros macros;

    @Column
    private String difficulty;

    @Column
    @Builder.Default
    private Boolean isAiGenerated = false;
} 