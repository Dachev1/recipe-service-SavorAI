package dev.idachev.recipeservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recipe_votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "recipe_id"})
})
@Getter
@ToString
@EqualsAndHashCode(of = {"userId", "recipeId"})
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RecipeVote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VoteType voteType;
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
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