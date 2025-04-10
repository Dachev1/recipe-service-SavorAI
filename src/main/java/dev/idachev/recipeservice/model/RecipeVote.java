package dev.idachev.recipeservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recipe_votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "recipe_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeVote {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VoteType voteType;
    
    @Column
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum VoteType {
        UPVOTE, DOWNVOTE
    }
} 