package dev.idachev.recipeservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Entity
@Table
@Getter
@ToString(exclude = {"macros", "tags"})
@EqualsAndHashCode(of = "id")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @Column
    private String title;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String ingredients;

    @Column(columnDefinition = "TEXT")
    private String servingSuggestions;

    @Column
    private UUID userId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

    @Column
    private Integer totalTimeMinutes;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "macros_id", referencedColumnName = "id")
    private Macros macros;

    @Column
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficulty;

    @Column
    @Builder.Default
    private Boolean isAiGenerated = false;
    
    @Column
    @Builder.Default
    private Integer upvotes = 0;
    
    @Column
    @Builder.Default
    private Integer downvotes = 0;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "recipe_tags", 
                     joinColumns = @JoinColumn(name = "recipe_id"),
                     indexes = { @Index(name = "idx_tag", columnList = "tag") })
    @Column(name = "tag")
    private List<String> tags;
} 